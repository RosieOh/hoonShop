package com.hoonshop.payment.application;

import com.hoonshop.common.domain.DomainException;
import com.hoonshop.payment.domain.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * 결제 승인 오케스트레이션.
 *
 * <p><b>이 클래스에는 {@code @Transactional}이 없습니다. 의도적입니다.</b>
 * 트랜잭션은 {@link PaymentTransactionService}가 필요한 구간에만 짧게 겁니다.
 * 여기에 트랜잭션을 걸면 PG 응답을 기다리는 동안 DB 커넥션과 재고 락을 붙잡게 되어,
 * PG가 느려지는 순간 사이트 전체가 멈춥니다.
 *
 * <p>흐름:
 * <pre>
 *   [Tx1 커밋] 멱등 확인 + 승인 시도 기록
 *        ↓
 *   [Tx 없음] PG 승인 호출 (타임아웃 있음)
 *        ↓
 *   [Tx2 커밋] 결과 반영 → 주문 결제완료 → 재고 차감
 * </pre>
 *
 * <p>각 단계의 실패를 다르게 다룹니다.
 * <ul>
 *   <li>PG가 거절 → FAILED. 확정된 실패이므로 재시도 가능.</li>
 *   <li>PG 타임아웃 → UNKNOWN. <b>실패로 처리하지 않습니다.</b> 승인됐을 수 있습니다.</li>
 *   <li>승인 후 금액 불일치 → 즉시 취소 시도. 잘못된 금액을 인정하지 않습니다.</li>
 * </ul>
 */
@Service
public class ConfirmPaymentService {

    private static final Logger log = LoggerFactory.getLogger(ConfirmPaymentService.class);

    private final PaymentTransactionService tx;
    private final PaymentGateway gateway;

    public ConfirmPaymentService(PaymentTransactionService tx, PaymentGateway gateway) {
        this.tx = tx;
        this.gateway = gateway;
    }

    public Receipt confirm(ConfirmPaymentCommand command) {
        // --- Tx1: 멱등 확인 + 시도 기록. PG를 부르기 전에 커밋된다. ---
        Payment payment = tx.beginOrGetExisting(command.idempotencyKey(), command.orderNumber(),
                command.customerEmail());

        if (payment.isFinished()) {
            // 재시도. 이미 처리된 결제이므로 두 번 청구하지 않습니다.
            log.info("멱등 재시도 — 기존 결과 반환: {}", command.idempotencyKey());
            return Receipt.from(payment);
        }
        if (payment.status() == PaymentStatus.FAILED) {
            throw new DomainException.Conflict("PAYMENT_ALREADY_FAILED",
                    "이미 실패한 결제 요청입니다. 새로 시도해 주세요.");
        }
        if (payment.status().needsReconciliation()) {
            throw new DomainException.Conflict("PAYMENT_UNDER_REVIEW",
                    "이전 결제 결과를 확인하는 중입니다. 잠시 후 다시 시도하거나 고객센터로 문의해 주세요.");
        }

        // --- 트랜잭션 밖: 외부 호출 ---
        PaymentGateway.Approval approval;
        try {
            approval = gateway.confirm(new PaymentGateway.ConfirmCommand(
                    payment.orderNumber(), command.paymentKey(), payment.requestedAmount()));

        } catch (GatewayTimeoutException e) {
            // 승인됐는지 알 수 없음. 실패로 단정하면 이미 빠져나간 돈을 놓칩니다.
            log.error("PG 응답 미확인 — 대사 필요. order={}", payment.orderNumber(), e);
            tx.settleUnknown(payment.id(), e.getMessage());
            throw new DomainException.Conflict("PAYMENT_RESULT_UNKNOWN",
                    "결제 결과를 확인하지 못했습니다. 중복 결제를 막기 위해 잠시 후 주문 내역에서 "
                            + "상태를 확인해 주세요. 이미 결제되었다면 자동으로 처리됩니다.");

        } catch (GatewayException e) {
            log.info("PG 승인 거절 — order={} code={}", payment.orderNumber(), e.code());
            tx.settleFailed(payment.id(), e.code(), e.getMessage());
            throw new DomainException.Conflict(e.code(), e.getMessage());
        }

        // --- Tx2: 결과 반영 + 주문/재고 (같은 트랜잭션) ---
        try {
            Payment settled = tx.settleApproved(payment.id(), approval);
            return Receipt.from(settled);

        } catch (DomainException.Conflict e) {
            // 승인은 났는데 우리 쪽에서 받아들일 수 없는 상황(금액 불일치, 재고 소진 등).
            // 고객 돈을 들고 있을 수 없으므로 즉시 되돌립니다.
            log.error("승인 후 처리 실패 — 결제를 취소합니다. order={} reason={}",
                    payment.orderNumber(), e.getMessage());
            compensate(approval, e.getMessage());
            throw e;
        }
    }

    /**
     * 보상 취소.
     *
     * <p>이것마저 실패하면 고객 돈을 들고 있는 상태가 되므로, 반드시 사람이 볼 수 있게
     * 남깁니다. 조용히 삼키면 아무도 모르는 채로 지나갑니다.
     */
    private void compensate(PaymentGateway.Approval approval, String reason) {
        try {
            gateway.cancel(approval.paymentKey(), approval.approvedAmount(),
                    "주문 처리 실패 보상 취소: " + reason,
                    "compensate-" + approval.paymentKey());
        } catch (RuntimeException cancelFailure) {
            log.error("!!! 보상 취소 실패 — 수동 환불 필요. paymentKey={} amount={}",
                    approval.paymentKey(), approval.approvedAmount(), cancelFailure);
        }
    }

    /**
     * @param paymentKey PG 결제창이 발급한 키. 카드번호가 아닙니다.
     */
    public record ConfirmPaymentCommand(String idempotencyKey, String orderNumber,
                                        String customerEmail, String paymentKey) {
    }

    public record Receipt(String orderId, String paymentKey, String method, long amount,
                          String status, Instant approvedAt, String receiptUrl,
                          VirtualAccountInfo virtualAccount) {

        static Receipt from(Payment payment) {
            VirtualAccountInfo va = payment.virtualAccountNumber() == null ? null
                    : new VirtualAccountInfo(payment.virtualAccountBank(),
                    payment.virtualAccountNumber(), payment.virtualAccountDueDate());

            return new Receipt(payment.orderNumber(), payment.paymentKey(),
                    payment.method() == null ? null : payment.method().name(),
                    payment.approvedAmount().value(), payment.status().name(),
                    payment.approvedAt(), payment.receiptUrl(), va);
        }
    }

    public record VirtualAccountInfo(String bank, String accountNumber, Instant dueDate) {
    }
}

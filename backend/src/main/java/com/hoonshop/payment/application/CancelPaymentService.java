package com.hoonshop.payment.application;

import com.hoonshop.common.domain.DomainException;
import com.hoonshop.common.domain.Money;
import com.hoonshop.payment.domain.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 결제 취소·환불.
 *
 * <p>승인과 마찬가지로 <b>PG 호출을 트랜잭션 밖에 둡니다.</b>
 *
 * <p>순서가 중요합니다: <b>PG 취소를 먼저 하고 그 다음에 우리 DB를 고칩니다.</b>
 * 반대로 하면 DB에는 "취소됨"인데 PG에서는 취소가 안 된 상태가 생기고,
 * 고객은 환불받지 못한 채 주문만 사라집니다. PG 취소가 성공한 뒤에 DB를 못 고치는 경우는
 * 원장에 흔적이 남아 대사로 잡을 수 있지만, 그 반대는 잡을 방법이 없습니다.
 */
@Service
public class CancelPaymentService {

    private static final Logger log = LoggerFactory.getLogger(CancelPaymentService.class);

    private final PaymentRepository payments;
    private final PaymentTransactionService tx;
    private final PaymentGateway gateway;

    public CancelPaymentService(PaymentRepository payments, PaymentTransactionService tx,
                                PaymentGateway gateway) {
        this.payments = payments;
        this.tx = tx;
        this.gateway = gateway;
    }

    /**
     * @param amount null이면 남은 전액 취소
     */
    public CancelResult cancel(String orderNumber, Money amount, String reason) {
        Payment payment = findPayment(orderNumber);
        Money target = amount == null ? payment.remainingAmount() : amount;

        if (target.isZero()) {
            throw new DomainException.Conflict("NOTHING_TO_CANCEL", "취소할 금액이 없습니다.");
        }
        // 도메인 규칙을 PG 호출 전에 미리 확인합니다 — 규칙 위반이면 굳이 PG를 부를 이유가 없습니다.
        if (target.value() > payment.remainingAmount().value()) {
            throw new DomainException.Conflict("CANCEL_AMOUNT_EXCEEDS",
                    "취소 가능 금액을 초과했습니다. 남은 금액: %s".formatted(payment.remainingAmount()));
        }

        // 1) PG 취소 먼저
        try {
            gateway.cancel(payment.paymentKey(), target, reason,
                    "cancel-%s-%d".formatted(orderNumber, payment.cancelledAmount().value()));
        } catch (GatewayTimeoutException e) {
            log.error("PG 취소 결과 미확인 — 수동 확인 필요. order={}", orderNumber, e);
            throw new DomainException.Conflict("CANCEL_RESULT_UNKNOWN",
                    "취소 요청은 접수됐지만 결과를 확인하지 못했습니다. 고객센터에서 확인해 드립니다.");
        } catch (GatewayException e) {
            throw new DomainException.Conflict(e.code(), e.getMessage());
        }

        // 2) 우리 DB 반영 (주문 취소·재고 복원은 PaymentCancelled 이벤트로 이어집니다)
        Payment cancelled = tx.applyCancellation(payment.id(), target, reason);

        return new CancelResult(orderNumber, cancelled.cancelledAmount().value(),
                cancelled.remainingAmount().value(), cancelled.status().name());
    }

    @Transactional(readOnly = true)
    public Payment findPayment(String orderNumber) {
        Payment payment = payments.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new DomainException.NotFound("PAYMENT_NOT_FOUND",
                        "결제 내역이 없습니다: " + orderNumber));

        if (payment.paymentKey() == null) {
            throw new DomainException.Conflict("NOT_APPROVED", "승인되지 않은 결제입니다.");
        }
        return payment;
    }

    public record CancelResult(String orderId, long cancelledAmount, long remainingAmount,
                               String status) {
    }
}

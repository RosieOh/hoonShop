package com.hoonshop.payment.application;

import com.hoonshop.common.domain.DomainException;
import com.hoonshop.common.domain.Money;
import com.hoonshop.payment.application.port.OrderAmountPort;
import com.hoonshop.payment.domain.*;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * 결제 승인 유스케이스.
 *
 * <p>순서가 중요합니다.
 * <ol>
 *   <li><b>멱등키 확인</b> — 재시도면 기존 결과를 그대로 돌려주고 끝냅니다.</li>
 *   <li><b>금액 검증</b> — 클라이언트가 보낸 금액이 아니라 주문에 기록된 금액으로 승인합니다.</li>
 *   <li><b>PG 승인</b> — 외부 호출.</li>
 *   <li><b>이벤트 발행</b> — 주문 상태 변경과 재고 차감이 이어집니다.</li>
 * </ol>
 *
 * <p>재고 차감을 커밋 이후로 미루지 않고 <b>같은 트랜잭션</b>에서 처리합니다.
 * 분리하면 "결제는 승인됐는데 재고는 안 빠진" 상태가 만들어지고, 그걸 되돌리려면
 * 보상 트랜잭션이 필요해집니다. 단일 DB를 쓰는 지금 규모에서는 한 트랜잭션이 정답입니다.
 */
@Service
public class ConfirmPaymentService {

    private final PaymentRepository payments;
    private final PaymentGateway gateway;
    private final OrderAmountPort orderAmounts;
    private final ApplicationEventPublisher events;

    public ConfirmPaymentService(PaymentRepository payments, PaymentGateway gateway,
                                 OrderAmountPort orderAmounts, ApplicationEventPublisher events) {
        this.payments = payments;
        this.gateway = gateway;
        this.orderAmounts = orderAmounts;
        this.events = events;
    }

    @Transactional
    public Receipt confirm(ConfirmPaymentCommand command) {
        // 1) 같은 요청이 이미 처리됐다면 두 번 청구하지 않습니다.
        var existing = payments.findByIdempotencyKey(command.idempotencyKey());
        if (existing.isPresent()) {
            Payment payment = existing.get();
            if (payment.isApproved()) {
                return Receipt.from(payment);
            }
            throw new DomainException.Conflict("PAYMENT_ALREADY_FAILED",
                    "이미 실패한 결제 요청입니다. 새로 시도해 주세요.");
        }

        // 2) 청구 금액은 주문에서 가져옵니다. 요청 본문의 금액은 신뢰하지 않습니다.
        Money payable = orderAmounts.payableOf(command.orderNumber(), command.customerEmail());

        Payment payment = Payment.request(command.idempotencyKey(), command.orderNumber(),
                command.method(), payable);

        // 3) PG 승인
        var result = gateway.approve(new PaymentGateway.ApprovalRequest(
                command.orderNumber(), command.method(), payable,
                command.paymentKey(), command.cardNumber()));

        if (!result.approved()) {
            payment.fail(result.failureCode(), result.failureMessage());
            payments.save(payment);
            throw new DomainException.Conflict(result.failureCode(), result.failureMessage());
        }

        payment.approve(result.paymentKey());
        payments.save(payment);

        // 4) 주문 상태 변경 → 재고 차감이 이 발행에서 연쇄로 일어납니다.
        payment.pollEvents().forEach(events::publishEvent);

        return Receipt.from(payment);
    }

    public record ConfirmPaymentCommand(
            String idempotencyKey,
            String orderNumber,
            String customerEmail,
            PaymentMethod method,
            String paymentKey,
            /** 데모 전용. 실연동에서는 카드번호가 서버로 오지 않습니다. */
            String cardNumber
    ) {
    }

    public record Receipt(String orderId, String paymentKey, String method, long amount,
                          Instant approvedAt, String status) {

        static Receipt from(Payment payment) {
            return new Receipt(payment.orderNumber(), payment.paymentKey(),
                    payment.method().name(), payment.amount().value(), payment.approvedAt(),
                    "DONE");
        }
    }
}

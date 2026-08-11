package com.hoonshop.payment.domain;

import com.hoonshop.common.domain.AggregateRoot;
import com.hoonshop.common.domain.DomainException;
import com.hoonshop.common.domain.Money;
import jakarta.persistence.*;

import java.time.Instant;

/**
 * 결제 애그리거트 루트.
 *
 * <p><b>멱등키가 이 애그리거트의 핵심입니다.</b> 결제 요청은 네트워크가 끊기면 클라이언트가
 * 재시도합니다. 키가 없으면 같은 주문이 두 번 승인되고 두 번 청구됩니다.
 * 키에 unique 제약을 걸어 두 번째 요청이 DB 수준에서 막히게 하고,
 * 애플리케이션은 기존 결과를 그대로 돌려줍니다.
 */
@Entity
@Table(name = "payment",
        uniqueConstraints = @UniqueConstraint(name = "uk_payment_idempotency",
                columnNames = "idempotency_key"))
public class Payment extends AggregateRoot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "idempotency_key", nullable = false, length = 80)
    private String idempotencyKey;

    @Column(name = "order_number", nullable = false, length = 24)
    private String orderNumber;

    @Column(name = "payment_key", length = 60)
    private String paymentKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentMethod method;

    @AttributeOverride(name = "amount", column = @Column(name = "amount", nullable = false))
    private Money amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status;

    @Column(name = "failure_code", length = 40)
    private String failureCode;

    @Column(name = "failure_message", length = 200)
    private String failureMessage;

    @Column(nullable = false)
    private Instant requestedAt;

    private Instant approvedAt;

    protected Payment() {
    }

    private Payment(String idempotencyKey, String orderNumber, PaymentMethod method, Money amount) {
        if (amount.isZero()) {
            throw new DomainException.Conflict("INVALID_AMOUNT", "결제 금액이 0원입니다.");
        }
        this.idempotencyKey = idempotencyKey;
        this.orderNumber = orderNumber;
        this.method = method;
        this.amount = amount;
        this.status = PaymentStatus.REQUESTED;
        this.requestedAt = Instant.now();
    }

    public static Payment request(String idempotencyKey, String orderNumber, PaymentMethod method,
                                  Money amount) {
        return new Payment(idempotencyKey, orderNumber, method, amount);
    }

    public void approve(String paymentKey) {
        if (status != PaymentStatus.REQUESTED) {
            throw new DomainException.Conflict("ALREADY_SETTLED",
                    "이미 처리된 결제입니다: " + status);
        }
        this.paymentKey = paymentKey;
        this.status = PaymentStatus.APPROVED;
        this.approvedAt = Instant.now();
        registerEvent(new PaymentApproved(orderNumber, paymentKey, amount, Instant.now()));
    }

    public void fail(String code, String message) {
        this.status = PaymentStatus.FAILED;
        this.failureCode = code;
        this.failureMessage = message;
    }

    public boolean isApproved() {
        return status == PaymentStatus.APPROVED;
    }

    public String idempotencyKey() {
        return idempotencyKey;
    }

    public String orderNumber() {
        return orderNumber;
    }

    public String paymentKey() {
        return paymentKey;
    }

    public PaymentMethod method() {
        return method;
    }

    public Money amount() {
        return amount;
    }

    public PaymentStatus status() {
        return status;
    }

    public String failureCode() {
        return failureCode;
    }

    public String failureMessage() {
        return failureMessage;
    }

    public Instant approvedAt() {
        return approvedAt;
    }

    public enum PaymentStatus {
        REQUESTED, APPROVED, FAILED, CANCELLED
    }
}

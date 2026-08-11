package com.hoonshop.payment.domain;

import com.hoonshop.common.domain.AggregateRoot;
import com.hoonshop.common.domain.DomainException;
import com.hoonshop.common.domain.Money;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * 결제 애그리거트 루트.
 *
 * <p>지키는 불변식:
 * <ul>
 *   <li>같은 멱등키로는 한 번만 청구된다 (DB UNIQUE + 애플리케이션 확인, 이중 방어)</li>
 *   <li>승인 금액은 요청 금액과 반드시 같다 — 다르면 승인을 인정하지 않는다</li>
 *   <li>취소 누적액은 승인 금액을 넘을 수 없다</li>
 *   <li>모든 상태 변화는 원장({@link PaymentLedgerEntry})에 남는다</li>
 * </ul>
 *
 * <p>원장을 따로 두는 이유: {@code status} 컬럼만 있으면 "지금 어떤 상태인가"는 알 수 있지만
 * "어떻게 여기까지 왔는가"는 알 수 없습니다. 결제 분쟁은 대부분 후자를 물어봅니다.
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

    @Column(name = "customer_email", nullable = false, length = 120)
    private String customerEmail;

    /** PG 결제창이 발급한 키. 카드번호가 아닙니다. */
    @Column(name = "payment_key", length = 200)
    private String paymentKey;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private PaymentMethod method;

    /** 서버가 계산한 청구 금액 — 승인 응답과 대조하는 기준값 */
    @AttributeOverride(name = "amount", column = @Column(name = "requested_amount", nullable = false))
    private Money requestedAmount;

    @AttributeOverride(name = "amount", column = @Column(name = "approved_amount", nullable = false))
    private Money approvedAmount = Money.ZERO;

    @AttributeOverride(name = "amount", column = @Column(name = "cancelled_amount", nullable = false))
    private Money cancelledAmount = Money.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private PaymentStatus status;

    @Column(name = "failure_code", length = 40)
    private String failureCode;

    @Column(name = "failure_message", length = 300)
    private String failureMessage;

    @Column(name = "receipt_url", length = 300)
    private String receiptUrl;

    @Column(name = "va_bank", length = 40)
    private String virtualAccountBank;

    @Column(name = "va_number", length = 40)
    private String virtualAccountNumber;

    @Column(name = "va_due_date")
    private Instant virtualAccountDueDate;

    @Column(nullable = false)
    private Instant requestedAt;

    private Instant approvedAt;

    /** 상태 변화 이력. 결제와 생명주기를 함께합니다. */
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id", nullable = false)
    @OrderBy("recordedAt ASC")
    private List<PaymentLedgerEntry> ledger = new ArrayList<>();

    @Version
    private Long version;

    protected Payment() {
    }

    private Payment(String idempotencyKey, String orderNumber, String customerEmail,
                    Money requestedAmount) {
        if (requestedAmount.isZero()) {
            throw new DomainException.Conflict("INVALID_AMOUNT", "결제 금액이 0원입니다.");
        }
        this.idempotencyKey = idempotencyKey;
        this.orderNumber = orderNumber;
        this.customerEmail = customerEmail;
        this.requestedAmount = requestedAmount;
        this.status = PaymentStatus.REQUESTED;
        this.requestedAt = Instant.now();
        record("REQUESTED", "승인 요청 준비", requestedAmount);
    }

    /**
     * 승인 요청 기록.
     *
     * <p><b>PG를 호출하기 전에</b> 이 레코드를 커밋합니다. 호출 도중 서버가 죽어도
     * "이 주문에 대해 승인을 시도했다"는 흔적이 남아야, 나중에 대사로 찾아낼 수 있습니다.
     * 호출 후에 기록하면 그 사이의 장애는 영원히 추적 불가능해집니다.
     */
    public static Payment request(String idempotencyKey, String orderNumber, String customerEmail,
                                  Money requestedAmount) {
        return new Payment(idempotencyKey, orderNumber, customerEmail, requestedAmount);
    }

    /**
     * PG 승인 결과 반영.
     *
     * <p>승인 금액을 반드시 대조합니다. PG 응답이 요청 금액과 다르면 무언가 잘못된 것이고,
     * 그대로 인정하면 잘못된 금액으로 상품이 나갑니다. 이 경우 승인을 받아들이지 않고
     * 예외를 던져 <b>즉시 취소 절차</b>로 넘깁니다.
     */
    public void approve(PaymentGateway.Approval approval) {
        if (status == PaymentStatus.APPROVED || status == PaymentStatus.WAITING_FOR_DEPOSIT) {
            return; // 멱등 — 같은 승인 결과가 두 번 들어와도 상태가 흔들리지 않습니다.
        }
        if (status == PaymentStatus.CANCELLED) {
            throw new DomainException.Conflict("ALREADY_CANCELLED", "이미 취소된 결제입니다.");
        }

        if (!approval.approvedAmount().equals(requestedAmount)) {
            record("AMOUNT_MISMATCH",
                    "승인 금액 불일치 (요청 %s / 승인 %s)".formatted(requestedAmount,
                            approval.approvedAmount()),
                    approval.approvedAmount());
            throw new DomainException.Conflict("PG_AMOUNT_MISMATCH",
                    "결제 승인 금액이 주문 금액과 다릅니다. 결제를 취소하고 다시 시도해 주세요.");
        }

        this.paymentKey = approval.paymentKey();
        this.method = approval.method();
        this.approvedAmount = approval.approvedAmount();
        this.receiptUrl = approval.receiptUrl();
        this.approvedAt = approval.approvedAt();

        if (approval.isWaitingForDeposit()) {
            // 가상계좌는 지금 돈이 들어온 게 아닙니다. 입금 웹훅을 받아야 완료됩니다.
            this.status = PaymentStatus.WAITING_FOR_DEPOSIT;
            PaymentGateway.VirtualAccount va = approval.virtualAccount();
            if (va != null) {
                this.virtualAccountBank = va.bank();
                this.virtualAccountNumber = va.accountNumber();
                this.virtualAccountDueDate = va.dueDate();
            }
            record("WAITING_FOR_DEPOSIT", "가상계좌 발급", approval.approvedAmount());
            return;
        }

        this.status = PaymentStatus.APPROVED;
        record("APPROVED", "PG 승인 완료", approval.approvedAmount());
        registerEvent(new PaymentApproved(orderNumber, paymentKey, approvedAmount, Instant.now()));
    }

    /** 가상계좌 입금 완료 (웹훅). 이 시점에 비로소 주문이 결제 완료가 됩니다. */
    public void confirmDeposit(Money depositedAmount) {
        if (status == PaymentStatus.APPROVED) {
            return; // 웹훅 재전송에 대한 멱등 처리
        }
        if (status != PaymentStatus.WAITING_FOR_DEPOSIT) {
            throw new DomainException.Conflict("NOT_WAITING_DEPOSIT",
                    "입금 대기 상태가 아닙니다: " + status.label());
        }
        if (!depositedAmount.equals(requestedAmount)) {
            record("DEPOSIT_MISMATCH", "입금 금액 불일치", depositedAmount);
            throw new DomainException.Conflict("DEPOSIT_AMOUNT_MISMATCH",
                    "입금 금액이 주문 금액과 다릅니다.");
        }
        this.status = PaymentStatus.APPROVED;
        this.approvedAt = Instant.now();
        record("DEPOSIT_CONFIRMED", "가상계좌 입금 확인", depositedAmount);
        registerEvent(new PaymentApproved(orderNumber, paymentKey, approvedAmount, Instant.now()));
    }

    /** PG가 명시적으로 거절. 결과가 확정된 실패입니다. */
    public void fail(String code, String message) {
        this.status = PaymentStatus.FAILED;
        this.failureCode = code;
        this.failureMessage = truncate(message);
        record("FAILED", "%s / %s".formatted(code, message), Money.ZERO);
    }

    /**
     * 결과를 알 수 없음.
     *
     * <p>이 상태의 결제는 사람이나 배치가 PG에 다시 물어봐야 합니다.
     * 절대로 실패로 뭉뚱그리면 안 됩니다 — 이미 승인됐을 수도 있습니다.
     */
    public void markUnknown(String reason) {
        this.status = PaymentStatus.UNKNOWN;
        this.failureMessage = truncate(reason);
        record("UNKNOWN", "결과 미확인 — 대사 필요: " + reason, Money.ZERO);
        registerEvent(new PaymentNeedsReconciliation(orderNumber, idempotencyKey, reason,
                Instant.now()));
    }

    /** 대사로 확정된 결과를 반영합니다. */
    public void resolveByReconciliation(PaymentGateway.Approval approval) {
        if (status != PaymentStatus.UNKNOWN) {
            throw new DomainException.Conflict("NOT_UNKNOWN", "대사 대상이 아닙니다.");
        }
        record("RECONCILED", "대사로 상태 확정", approval.approvedAmount());
        this.status = PaymentStatus.REQUESTED; // approve()가 상태 가드를 통과하도록
        approve(approval);
    }

    /**
     * 대사 결과 "승인되지 않음"으로 확정.
     *
     * <p>{@code REQUESTED}도 대상입니다 — PG를 부르기 직전에 서버가 죽으면 요청 기록만
     * 남고 멈추는데, 그것도 확정해줘야 영원히 미제로 남지 않습니다.
     */
    public void resolveAsFailed(String reason) {
        if (status != PaymentStatus.UNKNOWN && status != PaymentStatus.REQUESTED) {
            throw new DomainException.Conflict("NOT_RECONCILABLE", "대사 대상이 아닙니다.");
        }
        record("RECONCILED", "대사 결과: 승인되지 않음", Money.ZERO);
        fail("NOT_APPROVED", reason);
    }

    /**
     * 승인 취소.
     *
     * @param amount 취소 금액. 남은 금액보다 크면 거부합니다.
     */
    public void cancel(Money amount, String reason) {
        if (!status.isSettled() && status != PaymentStatus.WAITING_FOR_DEPOSIT) {
            throw new DomainException.Conflict("NOT_CANCELLABLE",
                    "취소할 수 없는 상태입니다: " + status.label());
        }
        Money remaining = approvedAmount.minusOrZero(cancelledAmount);
        if (amount.value() > remaining.value()) {
            throw new DomainException.Conflict("CANCEL_AMOUNT_EXCEEDS",
                    "취소 가능 금액을 초과했습니다. 남은 금액: %s".formatted(remaining));
        }

        this.cancelledAmount = cancelledAmount.plus(amount);
        boolean full = cancelledAmount.equals(approvedAmount);
        this.status = full ? PaymentStatus.CANCELLED : PaymentStatus.PARTIAL_CANCELLED;
        record(full ? "CANCELLED" : "PARTIAL_CANCELLED", reason, amount);
        registerEvent(new PaymentCancelled(orderNumber, paymentKey, amount, full, Instant.now()));
    }

    private void record(String type, String detail, Money amount) {
        ledger.add(PaymentLedgerEntry.of(type, truncate(detail), amount));
    }

    private String truncate(String value) {
        if (value == null) {
            return null;
        }
        return value.length() > 290 ? value.substring(0, 290) + "…" : value;
    }

    /* ------------------------------------------------------------ 접근자 --- */

    public Long id() {
        return id;
    }

    public boolean isApproved() {
        return status == PaymentStatus.APPROVED;
    }

    public boolean isFinished() {
        return status == PaymentStatus.APPROVED || status == PaymentStatus.WAITING_FOR_DEPOSIT;
    }

    public Money remainingAmount() {
        return approvedAmount.minusOrZero(cancelledAmount);
    }

    public String idempotencyKey() {
        return idempotencyKey;
    }

    public String orderNumber() {
        return orderNumber;
    }

    public String customerEmail() {
        return customerEmail;
    }

    public String paymentKey() {
        return paymentKey;
    }

    public PaymentMethod method() {
        return method;
    }

    public Money requestedAmount() {
        return requestedAmount;
    }

    public Money approvedAmount() {
        return approvedAmount;
    }

    public Money cancelledAmount() {
        return cancelledAmount;
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

    public String receiptUrl() {
        return receiptUrl;
    }

    public String virtualAccountBank() {
        return virtualAccountBank;
    }

    public String virtualAccountNumber() {
        return virtualAccountNumber;
    }

    public Instant virtualAccountDueDate() {
        return virtualAccountDueDate;
    }

    public Instant approvedAt() {
        return approvedAt;
    }

    public List<PaymentLedgerEntry> ledger() {
        return List.copyOf(ledger);
    }
}

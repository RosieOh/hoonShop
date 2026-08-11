package com.hoonshop.payment.domain;

import com.hoonshop.common.domain.Money;
import jakarta.persistence.*;

import java.time.Instant;

/**
 * 결제 원장 항목 — {@link Payment} 애그리거트 내부 엔티티.
 *
 * <p>결제 상태가 바뀔 때마다 한 줄씩 쌓입니다. 절대 수정하거나 삭제하지 않습니다(append-only).
 *
 * <p>왜 필요한가: 결제 분쟁은 "지금 무슨 상태냐"가 아니라 "어떻게 이렇게 됐냐"를 묻습니다.
 * status 컬럼 하나만 있으면 승인→취소된 결제와 처음부터 실패한 결제를 구분할 수 없고,
 * 타임아웃 후 대사로 확정된 건은 흔적조차 남지 않습니다.
 */
@Entity
@Table(name = "tbl_payment_ledger")
public class PaymentLedgerEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** REQUESTED · APPROVED · FAILED · UNKNOWN · CANCELLED · DEPOSIT_CONFIRMED … */
    @Column(name = "entry_type", nullable = false, length = 30)
    private String entryType;

    @Column(nullable = false, length = 300)
    private String detail;

    @AttributeOverride(name = "amount", column = @Column(name = "amount", nullable = false))
    private Money amount;

    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;

    protected PaymentLedgerEntry() {
    }

    private PaymentLedgerEntry(String entryType, String detail, Money amount) {
        this.entryType = entryType;
        this.detail = detail == null ? "" : detail;
        this.amount = amount;
        this.recordedAt = Instant.now();
    }

    static PaymentLedgerEntry of(String entryType, String detail, Money amount) {
        return new PaymentLedgerEntry(entryType, detail, amount);
    }

    public String entryType() {
        return entryType;
    }

    public String detail() {
        return detail;
    }

    public Money amount() {
        return amount;
    }

    public Instant recordedAt() {
        return recordedAt;
    }
}

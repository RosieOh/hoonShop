package com.hoonshop.common.domain;

import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

/**
 * 금액 값 객체 (원화 전용).
 *
 * <p>원화는 소수점이 없으므로 {@code long}으로 다룹니다. {@code double}은 0.1+0.2 문제로
 * 금액 계산에 절대 쓰면 안 되고, {@code BigDecimal}은 원화에서는 없는 소수부를 관리하느라
 * 스케일/반올림 규칙을 매번 신경 써야 합니다.
 *
 * <p>불변이며 음수를 허용하지 않습니다. 할인이 결제 금액을 넘는 상황은 계산 실수이지
 * "마이너스 금액"이 아니므로, 뺄셈은 0에서 잘리지 않고 예외를 던집니다 —
 * 조용히 0이 되면 잘못된 청구를 발견하지 못한 채 배포됩니다.
 */
@Embeddable
public final class Money implements Comparable<Money>, Serializable {

    public static final Money ZERO = new Money(0L);

    private long amount;

    protected Money() {
        // JPA 전용
    }

    private Money(long amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("금액은 음수일 수 없습니다: " + amount);
        }
        this.amount = amount;
    }

    public static Money won(long amount) {
        return amount == 0 ? ZERO : new Money(amount);
    }

    public long value() {
        return amount;
    }

    public Money plus(Money other) {
        return won(this.amount + other.amount);
    }

    public Money minus(Money other) {
        if (this.amount < other.amount) {
            throw new IllegalArgumentException(
                    "차감 금액이 원금액보다 큽니다: %d - %d".formatted(this.amount, other.amount));
        }
        return won(this.amount - other.amount);
    }

    /** 0 미만으로 내려가면 0으로 자릅니다. 할인 상한을 계산할 때만 씁니다. */
    public Money minusOrZero(Money other) {
        return this.amount <= other.amount ? ZERO : won(this.amount - other.amount);
    }

    public Money times(int quantity) {
        if (quantity < 0) {
            throw new IllegalArgumentException("수량은 음수일 수 없습니다: " + quantity);
        }
        return won(this.amount * quantity);
    }

    /**
     * 퍼센트 할인액. 원 단위 미만은 버립니다(고객에게 유리한 쪽이 아니라 사업자 관례를 따름).
     */
    public Money percentage(int percent) {
        if (percent < 0 || percent > 100) {
            throw new IllegalArgumentException("할인율은 0~100 사이여야 합니다: " + percent);
        }
        return won(this.amount * percent / 100);
    }

    public Money min(Money other) {
        return this.amount <= other.amount ? this : other;
    }

    public boolean isZero() {
        return amount == 0;
    }

    public boolean isGreaterThanOrEqual(Money other) {
        return this.amount >= other.amount;
    }

    public boolean isLessThan(Money other) {
        return this.amount < other.amount;
    }

    @Override
    public int compareTo(Money other) {
        return Long.compare(this.amount, other.amount);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Money money)) return false;
        return amount == money.amount;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(amount);
    }

    @Override
    public String toString() {
        return String.format("%,d원", amount);
    }
}

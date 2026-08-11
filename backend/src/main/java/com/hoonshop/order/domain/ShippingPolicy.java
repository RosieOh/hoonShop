package com.hoonshop.order.domain;

import com.hoonshop.common.domain.Money;

/**
 * 배송비 정책.
 *
 * <p>{@link Order} 안에 상수로 묻어두면 "쿠폰 할인액을 계산하려면 먼저 배송비를 알아야 하는데,
 * 배송비는 주문을 만들어야 알 수 있는" 순환이 생깁니다. 정책을 밖으로 꺼내
 * 주문 생성 전에도 같은 규칙을 쓸 수 있게 합니다.
 */
public final class ShippingPolicy {

    public static final Money FREE_THRESHOLD = Money.won(50_000);
    public static final Money BASE_FEE = Money.won(3_000);

    private ShippingPolicy() {
    }

    public static Money feeFor(Money payableItems) {
        if (payableItems.isZero()) {
            return Money.ZERO;
        }
        return payableItems.isGreaterThanOrEqual(FREE_THRESHOLD) ? Money.ZERO : BASE_FEE;
    }
}

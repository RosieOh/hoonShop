package com.hoonshop.order.application.port;

import com.hoonshop.common.domain.Money;

import java.util.List;

/**
 * 주문 → 프로모션 방향의 부패 방지 계층.
 *
 * <p>주문은 쿠폰의 종류나 중복 규칙을 알 필요가 없습니다. "이 코드들로 얼마가 깎이는가"만
 * 물어보고, 규칙 판단은 프로모션 컨텍스트에 맡깁니다.
 */
public interface CouponPort {

    Discount calculate(List<String> couponCodes, Money orderAmount, Money shippingFee);

    record Discount(Money itemDiscount, Money shippingDiscount) {
    }
}

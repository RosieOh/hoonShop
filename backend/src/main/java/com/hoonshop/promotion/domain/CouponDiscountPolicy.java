package com.hoonshop.promotion.domain;

import com.hoonshop.common.domain.DomainException;
import com.hoonshop.common.domain.Money;

import java.time.Instant;
import java.util.List;

/**
 * 쿠폰 조합 정책 — 도메인 서비스.
 *
 * <p>쿠폰 한 장으로는 답할 수 없는 질문("이 조합이 유효한가", "어떤 조합이 최대 할인인가")을
 * 다룹니다. 애그리거트에 넣을 수 없고 애플리케이션 서비스에 넣기엔 순수한 규칙이라
 * 도메인 서비스로 둡니다. 상태가 없으므로 정적 메서드로 충분합니다.
 *
 * <p>규칙: 상품할인 쿠폰은 <b>중복 가능 쿠폰 여러 장</b>이거나 <b>단독 쿠폰 한 장</b>입니다.
 * 배송비 쿠폰은 상품할인과 별개 축이라 항상 함께 쓸 수 있습니다.
 */
public final class CouponDiscountPolicy {

    private CouponDiscountPolicy() {
    }

    /** 조합 규칙 위반이면 예외. 결제 직전 서버가 반드시 다시 확인합니다. */
    public static void validateCombination(List<Coupon> coupons) {
        List<Coupon> itemCoupons = coupons.stream()
                .filter(c -> !c.appliesToShipping())
                .toList();

        boolean hasExclusive = itemCoupons.stream().anyMatch(c -> !c.isStackable());
        if (hasExclusive && itemCoupons.size() > 1) {
            throw new DomainException.Conflict("COUPON_NOT_STACKABLE",
                    "단독 사용 쿠폰은 다른 상품할인 쿠폰과 함께 쓸 수 없습니다.");
        }

        long shippingCount = coupons.stream().filter(Coupon::appliesToShipping).count();
        if (shippingCount > 1) {
            throw new DomainException.Conflict("COUPON_NOT_STACKABLE",
                    "배송비 쿠폰은 한 장만 사용할 수 있습니다.");
        }
    }

    /**
     * 적용 결과 합계.
     *
     * <p>상품 할인과 배송비 할인을 따로 셈합니다 — 합쳐버리면 배송비보다 큰 배송비 쿠폰이
     * 상품 금액까지 깎는 버그가 생깁니다.
     */
    public static DiscountBreakdown apply(List<Coupon> coupons, Money orderAmount,
                                          Money shippingFee, Instant now) {
        validateCombination(coupons);

        Money itemDiscount = coupons.stream()
                .filter(c -> !c.appliesToShipping())
                .map(c -> c.discountFor(orderAmount, shippingFee, now))
                .reduce(Money.ZERO, Money::plus)
                .min(orderAmount);

        Money shippingDiscount = coupons.stream()
                .filter(Coupon::appliesToShipping)
                .map(c -> c.discountFor(orderAmount, shippingFee, now))
                .reduce(Money.ZERO, Money::plus)
                .min(shippingFee);

        return new DiscountBreakdown(itemDiscount, shippingDiscount);
    }

    /**
     * 최대 할인 조합.
     *
     * <p>단독 쿠폰 중 최고액 한 장 vs 중복 가능 쿠폰 전부 — 둘을 비교합니다.
     * 이론적으로는 조합 최적화 문제지만, 실제 쿠폰 규칙에서 나올 수 있는 후보는 이 둘뿐입니다.
     */
    public static List<Coupon> findBestCombination(List<Coupon> candidates, Money orderAmount,
                                                   Money shippingFee, Instant now) {
        List<Coupon> usable = candidates.stream()
                .filter(c -> c.isUsableFor(orderAmount, shippingFee, now))
                .toList();

        List<Coupon> shipping = usable.stream().filter(Coupon::appliesToShipping).limit(1).toList();
        List<Coupon> stackables = usable.stream()
                .filter(c -> !c.appliesToShipping() && c.isStackable())
                .toList();

        Money stackTotal = stackables.stream()
                .map(c -> c.discountFor(orderAmount, shippingFee, now))
                .reduce(Money.ZERO, Money::plus);

        Coupon bestExclusive = usable.stream()
                .filter(c -> !c.appliesToShipping() && !c.isStackable())
                .max(java.util.Comparator.comparing(c -> c.discountFor(orderAmount, shippingFee, now)))
                .orElse(null);

        Money exclusiveTotal = bestExclusive == null
                ? Money.ZERO
                : bestExclusive.discountFor(orderAmount, shippingFee, now);

        List<Coupon> picked = exclusiveTotal.compareTo(stackTotal) > 0
                ? List.of(bestExclusive)
                : stackables;

        return java.util.stream.Stream.concat(picked.stream(), shipping.stream()).toList();
    }

    public record DiscountBreakdown(Money itemDiscount, Money shippingDiscount) {

        public Money total() {
            return itemDiscount.plus(shippingDiscount);
        }
    }
}

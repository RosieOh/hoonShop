package com.hoonshop.promotion.application;

import com.hoonshop.common.domain.Money;
import com.hoonshop.promotion.domain.Coupon;
import com.hoonshop.promotion.domain.CouponDiscountPolicy;
import com.hoonshop.promotion.domain.CouponRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class CouponService {

    private final CouponRepository coupons;

    public CouponService(CouponRepository coupons) {
        this.coupons = coupons;
    }

    /** 보유 쿠폰 목록. 사용 불가 쿠폰도 사유와 함께 내려줍니다. */
    public List<CouponView> myCoupons(Money orderAmount, Money shippingFee) {
        Instant now = Instant.now();
        return coupons.findAll().stream()
                .map(c -> CouponView.of(c, orderAmount, shippingFee, now))
                .toList();
    }

    /** 결제 시 서버가 다시 계산하는 할인액. 프론트가 보낸 금액은 참고용일 뿐입니다. */
    public CouponDiscountPolicy.DiscountBreakdown calculate(List<String> couponCodes,
                                                           Money orderAmount, Money shippingFee) {
        if (couponCodes == null || couponCodes.isEmpty()) {
            return new CouponDiscountPolicy.DiscountBreakdown(Money.ZERO, Money.ZERO);
        }
        List<Coupon> selected = coupons.findAllByCodes(couponCodes);
        return CouponDiscountPolicy.apply(selected, orderAmount, shippingFee, Instant.now());
    }

    public List<String> recommendBest(Money orderAmount, Money shippingFee) {
        return CouponDiscountPolicy
                .findBestCombination(coupons.findAll(), orderAmount, shippingFee, Instant.now())
                .stream()
                .map(Coupon::code)
                .toList();
    }

    public record CouponView(String id, String name, String type, int value, long minAmount,
                             long maxDiscount, Instant expiresAt, boolean stackable, String scope,
                             long discount, String unavailableReason) {

        static CouponView of(Coupon coupon, Money orderAmount, Money shippingFee, Instant now) {
            return new CouponView(
                    coupon.code(),
                    coupon.name(),
                    coupon.type().code(),
                    coupon.value(),
                    coupon.minAmount().value(),
                    coupon.maxDiscount().value(),
                    coupon.expiresAt(),
                    coupon.isStackable(),
                    coupon.appliesToShipping() ? "shipping" : "all",
                    coupon.discountFor(orderAmount, shippingFee, now).value(),
                    coupon.unavailableReason(orderAmount, shippingFee, now).orElse(null));
        }
    }
}

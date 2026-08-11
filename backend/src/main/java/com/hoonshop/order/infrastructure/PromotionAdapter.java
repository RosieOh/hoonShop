package com.hoonshop.order.infrastructure;

import com.hoonshop.common.domain.Money;
import com.hoonshop.order.application.port.CouponPort;
import com.hoonshop.promotion.application.CouponService;
import com.hoonshop.promotion.domain.CouponDiscountPolicy;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PromotionAdapter implements CouponPort {

    private final CouponService couponService;

    public PromotionAdapter(CouponService couponService) {
        this.couponService = couponService;
    }

    @Override
    public Discount calculate(List<String> couponCodes, Money orderAmount, Money shippingFee) {
        CouponDiscountPolicy.DiscountBreakdown breakdown =
                couponService.calculate(couponCodes, orderAmount, shippingFee);
        return new Discount(breakdown.itemDiscount(), breakdown.shippingDiscount());
    }
}

package com.hoonshop.promotion.presentation;

import com.hoonshop.common.domain.Money;
import com.hoonshop.promotion.application.CouponService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Coupon", description = "쿠폰")
@RestController
@RequestMapping("/api/coupons")
public class CouponController {

    private final CouponService couponService;

    public CouponController(CouponService couponService) {
        this.couponService = couponService;
    }

    @Operation(summary = "보유 쿠폰 목록 (주문 금액 기준 할인액·사용 불가 사유 포함)")
    @GetMapping
    public CouponListResponse list(
            @RequestParam(defaultValue = "0") long orderAmount,
            @RequestParam(defaultValue = "0") long shippingFee) {
        return new CouponListResponse(
                couponService.myCoupons(Money.won(orderAmount), Money.won(shippingFee)));
    }

    @Operation(summary = "최대 할인 조합 추천")
    @GetMapping("/best")
    public BestCouponResponse best(
            @RequestParam(defaultValue = "0") long orderAmount,
            @RequestParam(defaultValue = "0") long shippingFee) {
        return new BestCouponResponse(
                couponService.recommendBest(Money.won(orderAmount), Money.won(shippingFee)));
    }

    public record CouponListResponse(List<CouponService.CouponView> items) {
    }

    public record BestCouponResponse(List<String> couponIds) {
    }
}

package com.hoonshop.promotion.domain;

import java.util.List;

public interface CouponRepository {

    List<Coupon> findAll();

    List<Coupon> findAllByCodes(List<String> codes);

    Coupon save(Coupon coupon);
}

package com.hoonshop.promotion.infrastructure;

import com.hoonshop.promotion.domain.Coupon;
import com.hoonshop.promotion.domain.CouponRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class CouponRepositoryAdapter implements CouponRepository {

    private final CouponJpaRepository jpa;

    public CouponRepositoryAdapter(CouponJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public List<Coupon> findAll() {
        return jpa.findAll();
    }

    @Override
    public List<Coupon> findAllByCodes(List<String> codes) {
        return codes.isEmpty() ? List.of() : jpa.findByCodeIn(codes);
    }

    @Override
    public Coupon save(Coupon coupon) {
        return jpa.save(coupon);
    }
}

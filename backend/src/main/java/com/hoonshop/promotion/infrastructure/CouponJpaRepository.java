package com.hoonshop.promotion.infrastructure;

import com.hoonshop.promotion.domain.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CouponJpaRepository extends JpaRepository<Coupon, Long> {

    List<Coupon> findByCodeIn(List<String> codes);
}

package com.hoonshop.promotion.domain;

import com.hoonshop.common.domain.DomainException;
import com.hoonshop.common.domain.Money;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("쿠폰 할인 정책")
class CouponDiscountPolicyTest {

    private static final Instant NOW = Instant.parse("2026-08-11T00:00:00Z");
    private static final Instant FUTURE = NOW.plus(30, ChronoUnit.DAYS);
    private static final Instant PAST = NOW.minus(1, ChronoUnit.DAYS);

    private Coupon percent(String code, int rate, long min, long max, boolean stackable) {
        return Coupon.create(code, code, DiscountType.PERCENT, rate, Money.won(min),
                Money.won(max), FUTURE, stackable);
    }

    private Coupon amount(String code, long value, long min, boolean stackable) {
        return Coupon.create(code, code, DiscountType.AMOUNT, (int) value, Money.won(min),
                Money.won(value), FUTURE, stackable);
    }

    private Coupon shipping(String code) {
        return Coupon.create(code, code, DiscountType.SHIPPING, 3_000, Money.ZERO,
                Money.won(3_000), FUTURE, true);
    }

    @Nested
    @DisplayName("사용 가능 여부")
    class Availability {

        @Test
        @DisplayName("최소 주문금액에 미달하면 얼마가 모자란지 알려준다")
        void tellsHowMuchMoreIsNeeded() {
            Coupon coupon = percent("CPN", 15, 20_000, 8_000, false);

            assertThat(coupon.unavailableReason(Money.won(12_000), Money.won(3_000), NOW))
                    .contains("8,000원 더 담으면 사용 가능해요");
        }

        @Test
        @DisplayName("만료된 쿠폰은 사유를 알려준다")
        void reportsExpiry() {
            Coupon expired = Coupon.create("OLD", "OLD", DiscountType.PERCENT, 10,
                    Money.ZERO, Money.won(5_000), PAST, false);

            assertThat(expired.unavailableReason(Money.won(50_000), Money.won(0), NOW))
                    .contains("유효기간이 지났어요");
        }

        @Test
        @DisplayName("이미 무료배송이면 배송비 쿠폰은 쓸 수 없다")
        void shippingCouponUselessWhenFree() {
            // Optional.contains는 부분 문자열이 아니라 정확히 같은 값을 요구합니다.
            assertThat(shipping("SHIP").unavailableReason(Money.won(60_000), Money.ZERO, NOW))
                    .contains("이미 무료배송이라 적용할 수 없어요");
        }
    }

    @Nested
    @DisplayName("할인액")
    class DiscountAmount {

        @Test
        @DisplayName("퍼센트 할인은 상한을 넘지 않는다")
        void capsPercentDiscount() {
            Coupon coupon = percent("CPN", 15, 20_000, 8_000, false);

            // 100,000 × 15% = 15,000 이지만 상한 8,000
            assertThat(coupon.discountFor(Money.won(100_000), Money.won(0), NOW))
                    .isEqualTo(Money.won(8_000));
        }

        @Test
        @DisplayName("사용 조건을 못 채운 쿠폰의 할인액은 0원이다")
        void zeroWhenUnusable() {
            Coupon coupon = percent("CPN", 15, 50_000, 8_000, false);

            assertThat(coupon.discountFor(Money.won(10_000), Money.won(3_000), NOW))
                    .isEqualTo(Money.ZERO);
        }
    }

    @Nested
    @DisplayName("조합 규칙")
    class Combination {

        @Test
        @DisplayName("단독 사용 쿠폰 두 장은 함께 쓸 수 없다")
        void rejectsTwoExclusives() {
            List<Coupon> selected = List.of(
                    percent("A", 10, 0, 5_000, false),
                    percent("B", 15, 0, 8_000, false));

            assertThatThrownBy(() -> CouponDiscountPolicy.validateCombination(selected))
                    .isInstanceOf(DomainException.Conflict.class)
                    .hasMessageContaining("단독 사용 쿠폰");
        }

        @Test
        @DisplayName("단독 쿠폰 + 배송비 쿠폰은 함께 쓸 수 있다")
        void allowsExclusiveWithShipping() {
            CouponDiscountPolicy.validateCombination(List.of(
                    percent("A", 10, 0, 5_000, false),
                    shipping("SHIP")));
        }

        @Test
        @DisplayName("중복 가능 쿠폰은 여러 장 쓸 수 있다")
        void allowsMultipleStackables() {
            CouponDiscountPolicy.validateCombination(List.of(
                    amount("A", 3_000, 0, true),
                    amount("B", 2_000, 0, true)));
        }
    }

    @Nested
    @DisplayName("적용 결과")
    class Apply {

        @Test
        @DisplayName("배송비 할인은 배송비를 넘지 않고, 상품 할인은 주문금액을 넘지 않는다")
        void clampsBothAxes() {
            var result = CouponDiscountPolicy.apply(
                    List.of(amount("BIG", 99_999, 0, true), shipping("SHIP")),
                    Money.won(10_000), Money.won(3_000), NOW);

            assertThat(result.itemDiscount()).isEqualTo(Money.won(10_000));
            assertThat(result.shippingDiscount()).isEqualTo(Money.won(3_000));
        }
    }

    @Nested
    @DisplayName("최대 할인 조합 찾기")
    class BestCombination {

        @Test
        @DisplayName("단독 쿠폰이 더 크면 그것 하나를 고른다")
        void picksExclusiveWhenBigger() {
            List<Coupon> candidates = List.of(
                    percent("BIG", 20, 0, 20_000, false),   // 100,000의 20% = 20,000
                    amount("SMALL1", 3_000, 0, true),
                    amount("SMALL2", 2_000, 0, true));      // 합쳐도 5,000

            List<String> best = CouponDiscountPolicy
                    .findBestCombination(candidates, Money.won(100_000), Money.ZERO, NOW)
                    .stream().map(Coupon::code).toList();

            assertThat(best).containsExactly("BIG");
        }

        @Test
        @DisplayName("중복 쿠폰 합계가 더 크면 그쪽을 전부 고른다")
        void picksStackablesWhenBigger() {
            List<Coupon> candidates = List.of(
                    percent("SMALL", 1, 0, 1_000, false),
                    amount("A", 5_000, 0, true),
                    amount("B", 4_000, 0, true));

            List<String> best = CouponDiscountPolicy
                    .findBestCombination(candidates, Money.won(100_000), Money.ZERO, NOW)
                    .stream().map(Coupon::code).toList();

            assertThat(best).containsExactlyInAnyOrder("A", "B");
        }

        @Test
        @DisplayName("배송비 쿠폰은 상품 쿠폰과 무관하게 항상 포함된다")
        void alwaysIncludesShippingCoupon() {
            List<Coupon> candidates = List.of(
                    percent("BIG", 20, 0, 20_000, false),
                    shipping("SHIP"));

            List<String> best = CouponDiscountPolicy
                    .findBestCombination(candidates, Money.won(30_000), Money.won(3_000), NOW)
                    .stream().map(Coupon::code).toList();

            assertThat(best).containsExactlyInAnyOrder("BIG", "SHIP");
        }

        @Test
        @DisplayName("만료된 쿠폰은 후보에서 빠진다")
        void excludesExpired() {
            Coupon expired = Coupon.create("OLD", "OLD", DiscountType.AMOUNT, 9_000,
                    Money.ZERO, Money.won(9_000), PAST, true);

            List<String> best = CouponDiscountPolicy
                    .findBestCombination(List.of(expired), Money.won(50_000), Money.ZERO, NOW)
                    .stream().map(Coupon::code).toList();

            assertThat(best).isEmpty();
        }
    }
}

package com.hoonshop.promotion.domain;

import com.hoonshop.common.domain.AggregateRoot;
import com.hoonshop.common.domain.Money;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.Optional;

/**
 * 쿠폰 애그리거트 루트.
 *
 * <p>"이 쿠폰을 지금 쓸 수 있는가"와 "얼마가 할인되는가"는 쿠폰 자신의 규칙이므로
 * 여기에 둡니다. 여러 장을 어떻게 조합할지는 쿠폰 하나로는 판단할 수 없어
 * {@link CouponDiscountPolicy}(도메인 서비스)가 맡습니다 —
 * 애그리거트 하나에 담기지 않는 규칙이 도메인 서비스가 필요한 전형적인 신호입니다.
 */
@Entity
@Table(name = "tbl_coupon")
public class Coupon extends AggregateRoot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 40)
    private String code;

    @Column(nullable = false, length = 80)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DiscountType type;

    /** PERCENT면 할인율(%), 그 외에는 금액(원). */
    @Column(nullable = false)
    private int value;

    @AttributeOverride(name = "amount", column = @Column(name = "min_amount", nullable = false))
    private Money minAmount;

    @AttributeOverride(name = "amount", column = @Column(name = "max_discount", nullable = false))
    private Money maxDiscount;

    @Column(nullable = false)
    private Instant expiresAt;

    /** false면 다른 상품할인 쿠폰과 함께 쓸 수 없습니다. */
    @Column(nullable = false)
    private boolean stackable;

    protected Coupon() {
    }

    private Coupon(String code, String name, DiscountType type, int value, Money minAmount,
                   Money maxDiscount, Instant expiresAt, boolean stackable) {
        if (type == DiscountType.PERCENT && (value <= 0 || value > 100)) {
            throw new IllegalArgumentException("할인율은 1~100 사이여야 합니다: " + value);
        }
        if (type != DiscountType.PERCENT && value <= 0) {
            throw new IllegalArgumentException("할인 금액은 1원 이상이어야 합니다: " + value);
        }
        this.code = code;
        this.name = name;
        this.type = type;
        this.value = value;
        this.minAmount = minAmount;
        this.maxDiscount = maxDiscount;
        this.expiresAt = expiresAt;
        this.stackable = stackable;
    }

    public static Coupon create(String code, String name, DiscountType type, int value,
                                Money minAmount, Money maxDiscount, Instant expiresAt,
                                boolean stackable) {
        return new Coupon(code, name, type, value, minAmount, maxDiscount, expiresAt, stackable);
    }

    public boolean isExpired(Instant now) {
        return expiresAt.isBefore(now);
    }

    /**
     * 사용 불가 사유. 쓸 수 있으면 비어 있습니다.
     *
     * <p>불리언이 아니라 사유를 돌려주는 이유: 화면에서 "왜 못 쓰는지"를 보여줘야 하는데,
     * 그 판단을 프론트가 다시 하면 서버와 규칙이 갈라집니다.
     */
    public Optional<String> unavailableReason(Money orderAmount, Money shippingFee, Instant now) {
        if (isExpired(now)) {
            return Optional.of("유효기간이 지났어요");
        }
        if (orderAmount.isLessThan(minAmount)) {
            return Optional.of("%,d원 더 담으면 사용 가능해요"
                    .formatted(minAmount.minus(orderAmount).value()));
        }
        if (type == DiscountType.SHIPPING && shippingFee.isZero()) {
            return Optional.of("이미 무료배송이라 적용할 수 없어요");
        }
        return Optional.empty();
    }

    public boolean isUsableFor(Money orderAmount, Money shippingFee, Instant now) {
        return unavailableReason(orderAmount, shippingFee, now).isEmpty();
    }

    /** 실제 할인액. 쓸 수 없는 상황이면 0원입니다. */
    public Money discountFor(Money orderAmount, Money shippingFee, Instant now) {
        if (!isUsableFor(orderAmount, shippingFee, now)) {
            return Money.ZERO;
        }
        return switch (type) {
            case PERCENT -> orderAmount.percentage(value).min(maxDiscount);
            case AMOUNT -> Money.won(value).min(orderAmount);
            case SHIPPING -> Money.won(value).min(shippingFee);
        };
    }

    public boolean appliesToShipping() {
        return type == DiscountType.SHIPPING;
    }

    public String code() {
        return code;
    }

    public String name() {
        return name;
    }

    public DiscountType type() {
        return type;
    }

    public int value() {
        return value;
    }

    public Money minAmount() {
        return minAmount;
    }

    public Money maxDiscount() {
        return maxDiscount;
    }

    public Instant expiresAt() {
        return expiresAt;
    }

    public boolean isStackable() {
        return stackable;
    }
}

package com.hoonshop.order.domain;

import com.hoonshop.common.domain.Money;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;

/**
 * 주문 금액 내역.
 *
 * <p>최종 결제액 하나만 저장하지 않고 구성 요소를 모두 남깁니다.
 * 나중에 "왜 이 금액이 나왔는지" 물어봤을 때 재계산으로 답하면, 그 사이 정책이 바뀌었을 경우
 * 다른 답이 나옵니다. 영수증은 그때의 계산 과정 자체를 보관해야 합니다.
 */
@Embeddable
public final class OrderAmounts implements Serializable {

    @AttributeOverride(name = "amount", column = @Column(name = "items_list_total", nullable = false))
    private Money itemsListTotal;

    @AttributeOverride(name = "amount", column = @Column(name = "item_discount", nullable = false))
    private Money itemDiscount;

    @AttributeOverride(name = "amount", column = @Column(name = "coupon_discount", nullable = false))
    private Money couponDiscount;

    @AttributeOverride(name = "amount", column = @Column(name = "shipping_fee", nullable = false))
    private Money shippingFee;

    @AttributeOverride(name = "amount", column = @Column(name = "shipping_discount", nullable = false))
    private Money shippingDiscount;

    @AttributeOverride(name = "amount", column = @Column(name = "payable", nullable = false))
    private Money payable;

    protected OrderAmounts() {
    }

    private OrderAmounts(Money itemsListTotal, Money itemDiscount, Money couponDiscount,
                         Money shippingFee, Money shippingDiscount) {
        this.itemsListTotal = itemsListTotal;
        this.itemDiscount = itemDiscount;
        this.couponDiscount = couponDiscount;
        this.shippingFee = shippingFee;
        this.shippingDiscount = shippingDiscount;
        this.payable = itemsListTotal
                .minusOrZero(itemDiscount)
                .minusOrZero(couponDiscount)
                .plus(shippingFee)
                .minusOrZero(shippingDiscount);
    }

    public static OrderAmounts of(Money itemsListTotal, Money itemDiscount, Money couponDiscount,
                                  Money shippingFee, Money shippingDiscount) {
        return new OrderAmounts(itemsListTotal, itemDiscount, couponDiscount, shippingFee,
                shippingDiscount);
    }

    public Money itemsListTotal() {
        return itemsListTotal;
    }

    public Money itemDiscount() {
        return itemDiscount;
    }

    public Money couponDiscount() {
        return couponDiscount;
    }

    public Money shippingFee() {
        return shippingFee;
    }

    public Money shippingDiscount() {
        return shippingDiscount;
    }

    public Money payable() {
        return payable;
    }
}

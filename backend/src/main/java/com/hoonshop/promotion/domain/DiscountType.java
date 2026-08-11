package com.hoonshop.promotion.domain;

public enum DiscountType {
    /** 주문 금액의 n% (상한 있음) */
    PERCENT("percent"),
    /** 정액 할인 */
    AMOUNT("amount"),
    /** 배송비만 할인 */
    SHIPPING("shipping");

    private final String code;

    DiscountType(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }
}

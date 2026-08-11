package com.hoonshop.payment.domain;

public enum PaymentMethod {
    CARD("신용·체크카드"),
    TRANSFER("실시간 계좌이체"),
    VIRTUAL("가상계좌"),
    EASY("간편결제");

    private final String label;

    PaymentMethod(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}

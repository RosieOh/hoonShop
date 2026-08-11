package com.hoonshop.order.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.time.Year;
import java.util.Objects;

/** 주문번호 (ORD-2026-00001). 고객이 상담 시 불러주는 값이라 읽기 쉬워야 합니다. */
@Embeddable
public final class OrderNumber implements Serializable {

    @Column(name = "order_number", nullable = false, unique = true, length = 24)
    private String value;

    protected OrderNumber() {
    }

    private OrderNumber(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("주문번호는 비어 있을 수 없습니다.");
        }
        this.value = value;
    }

    public static OrderNumber of(String value) {
        return new OrderNumber(value);
    }

    public static OrderNumber issue(long sequence) {
        return new OrderNumber("ORD-%d-%05d".formatted(Year.now().getValue(), sequence));
    }

    public String value() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof OrderNumber other)) return false;
        return Objects.equals(value, other.value);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(value);
    }

    @Override
    public String toString() {
        return value;
    }
}

package com.hoonshop.catalog.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * 상품 코드 (P0001).
 *
 * <p>DB의 auto-increment id와 별개로 둡니다. 대외 식별자를 PK로 그대로 노출하면
 * 총 상품 수가 새어나가고, 나중에 데이터를 이관할 때 식별자가 바뀝니다.
 */
@Embeddable
public final class ProductCode implements Serializable {

    private static final Pattern FORMAT = Pattern.compile("^P\\d{4}$");

    @Column(name = "code", nullable = false, length = 16)
    private String value;

    protected ProductCode() {
    }

    private ProductCode(String value) {
        if (value == null || !FORMAT.matcher(value).matches()) {
            throw new IllegalArgumentException("상품 코드 형식이 올바르지 않습니다: " + value);
        }
        this.value = value;
    }

    public static ProductCode of(String value) {
        return new ProductCode(value);
    }

    public static ProductCode ofSequence(int sequence) {
        return new ProductCode("P%04d".formatted(sequence));
    }

    public String value() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ProductCode other)) return false;
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

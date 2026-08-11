package com.hoonshop.catalog.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;
import java.util.regex.Pattern;

/** 컬러 옵션 (id/라벨/hex). 주문 시 선택한 옵션을 검증하는 근거가 됩니다. */
@Embeddable
public class ColorOption implements Serializable {

    private static final Pattern HEX = Pattern.compile("^#[0-9A-Fa-f]{6}$");

    @Column(name = "color_id", nullable = false, length = 32)
    private String id;

    @Column(name = "color_label", nullable = false, length = 32)
    private String label;

    @Column(name = "color_hex", nullable = false, length = 7)
    private String hex;

    protected ColorOption() {
    }

    private ColorOption(String id, String label, String hex) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("컬러 id는 비어 있을 수 없습니다.");
        }
        if (hex == null || !HEX.matcher(hex).matches()) {
            throw new IllegalArgumentException("컬러 hex 형식이 올바르지 않습니다: " + hex);
        }
        this.id = id;
        this.label = label;
        this.hex = hex;
    }

    public static ColorOption of(String id, String label, String hex) {
        return new ColorOption(id, label, hex);
    }

    public String id() {
        return id;
    }

    public String label() {
        return label;
    }

    public String hex() {
        return hex;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ColorOption other)) return false;
        return Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}

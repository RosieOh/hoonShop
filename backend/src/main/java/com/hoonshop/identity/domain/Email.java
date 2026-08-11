package com.hoonshop.identity.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * 이메일 값 객체.
 *
 * <p>문자열로 들고 다니면 검증하지 않은 값이 시스템 안쪽까지 들어옵니다.
 * 생성 시점에 한 번 검증하고 나면 이후로는 유효하다고 믿을 수 있습니다.
 * 대소문자는 생성 시 소문자로 정규화합니다 — 그러지 않으면 같은 사람이 두 계정을 갖게 됩니다.
 */
@Embeddable
public final class Email implements Serializable {

    private static final Pattern FORMAT = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]{2,}$");

    @Column(name = "email", nullable = false, unique = true, length = 120)
    private String value;

    protected Email() {
    }

    private Email(String value) {
        if (value == null || !FORMAT.matcher(value.trim()).matches()) {
            throw new IllegalArgumentException("이메일 형식이 올바르지 않습니다: " + value);
        }
        this.value = value.trim().toLowerCase();
    }

    public static Email of(String value) {
        return new Email(value);
    }

    public String value() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Email other)) return false;
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

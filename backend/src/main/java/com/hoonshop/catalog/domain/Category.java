package com.hoonshop.catalog.domain;

import java.util.Arrays;

public enum Category {
    NECKLACE("necklace", "목걸이"),
    BRACELET("bracelet", "팔찌"),
    EARRING("earring", "귀걸이"),
    RING("ring", "반지"),
    ANKLET("anklet", "발찌"),
    STRAP("strap", "키링·스트랩");

    private final String code;
    private final String label;

    Category(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String code() {
        return code;
    }

    public String label() {
        return label;
    }

    /** API는 소문자 코드를 쓰므로 enum 이름이 아니라 코드로 찾습니다. */
    public static Category fromCode(String code) {
        return Arrays.stream(values())
                .filter(c -> c.code.equalsIgnoreCase(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("알 수 없는 카테고리: " + code));
    }
}

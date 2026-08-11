package com.hoonshop.identity.domain;

import java.util.Arrays;

public enum SocialProvider {
    KAKAO("kakao", "카카오"),
    NAVER("naver", "네이버"),
    GOOGLE("google", "구글");

    private final String code;
    private final String label;

    SocialProvider(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String code() {
        return code;
    }

    public String label() {
        return label;
    }

    public static SocialProvider fromCode(String code) {
        return Arrays.stream(values())
                .filter(p -> p.code.equalsIgnoreCase(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("지원하지 않는 로그인 방식입니다: " + code));
    }
}

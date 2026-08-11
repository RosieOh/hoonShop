package com.hoonshop.identity.domain;

public enum Role {
    CUSTOMER("customer"),
    ADMIN("admin");

    private final String code;

    Role(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }

    /** Spring Security 권한 문자열. */
    public String authority() {
        return "ROLE_" + name();
    }
}

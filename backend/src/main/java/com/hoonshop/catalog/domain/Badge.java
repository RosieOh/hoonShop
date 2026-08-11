package com.hoonshop.catalog.domain;

public enum Badge {
    NEW("new"),
    BEST("best"),
    LIMITED("limited");

    private final String code;

    Badge(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }
}

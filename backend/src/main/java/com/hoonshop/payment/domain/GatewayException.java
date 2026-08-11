package com.hoonshop.payment.domain;

/** PG가 명시적으로 거절했을 때. 결과가 확정된 실패입니다. */
public class GatewayException extends RuntimeException {

    private final String code;

    public GatewayException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String code() {
        return code;
    }
}

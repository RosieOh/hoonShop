package com.hoonshop.payment.domain;

/**
 * PG 호출 결과를 알 수 없을 때.
 *
 * <p>{@link GatewayException}(확정된 거절)과 반드시 구분해야 합니다.
 * 타임아웃을 실패로 처리하면, 실제로는 승인된 결제를 "실패"로 기록하게 됩니다 —
 * 고객 카드에서는 돈이 빠져나갔는데 우리 시스템에는 주문이 없습니다.
 * 이 예외를 받으면 결제를 {@link PaymentStatus#UNKNOWN}으로 남기고 대사로 확정합니다.
 */
public class GatewayTimeoutException extends RuntimeException {

    public GatewayTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }
}

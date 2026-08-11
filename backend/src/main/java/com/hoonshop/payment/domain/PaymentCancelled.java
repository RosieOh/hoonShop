package com.hoonshop.payment.domain;

import com.hoonshop.common.domain.DomainEvent;
import com.hoonshop.common.domain.Money;

import java.time.Instant;

/**
 * 결제가 취소됨.
 *
 * <p>{@code full}이 true일 때만 주문이 취소되고 재고가 복원됩니다.
 * 부분 취소는 주문이 살아 있으므로 재고를 되돌리면 안 됩니다.
 */
public record PaymentCancelled(String orderNumber, String paymentKey, Money cancelledAmount,
                               boolean full, Instant occurredAt) implements DomainEvent {
}

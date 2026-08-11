package com.hoonshop.payment.domain;

import com.hoonshop.common.domain.DomainEvent;
import com.hoonshop.common.domain.Money;

import java.time.Instant;

/** PG 승인 완료. 주문 컨텍스트가 받아 주문을 결제 완료로 바꿉니다. */
public record PaymentApproved(String orderNumber, String paymentKey, Money amount,
                              Instant occurredAt) implements DomainEvent {
}

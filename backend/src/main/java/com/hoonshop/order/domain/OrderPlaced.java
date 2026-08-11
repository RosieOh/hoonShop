package com.hoonshop.order.domain;

import com.hoonshop.common.domain.DomainEvent;
import com.hoonshop.common.domain.Money;

import java.time.Instant;

/** 주문서가 만들어짐. 아직 결제 전이라 재고는 건드리지 않습니다. */
public record OrderPlaced(OrderNumber orderNumber, String customerEmail, Money payable,
                          Instant occurredAt) implements DomainEvent {
}

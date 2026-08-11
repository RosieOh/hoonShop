package com.hoonshop.order.domain;

import com.hoonshop.common.domain.DomainEvent;

import java.time.Instant;
import java.util.List;

/**
 * 주문이 취소됨.
 *
 * <p>{@code wasPaid}가 true일 때만 재고를 되돌립니다 — 결제 전 취소는 애초에 재고를
 * 빼지 않았으므로 복원하면 없던 재고가 생깁니다.
 */
public record OrderCancelled(OrderNumber orderNumber, List<OrderPaid.StockChange> stockChanges,
                             boolean wasPaid, String reason, Instant occurredAt)
        implements DomainEvent {
}

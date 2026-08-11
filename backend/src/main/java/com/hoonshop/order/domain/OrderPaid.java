package com.hoonshop.order.domain;

import com.hoonshop.common.domain.DomainEvent;

import java.time.Instant;
import java.util.List;

/**
 * 결제가 승인됨.
 *
 * <p>catalog 컨텍스트가 이 이벤트를 받아 재고를 뺍니다. order가 catalog를 직접 호출하지 않는 덕에
 * 두 컨텍스트는 서로의 내부 모델을 모릅니다 — 이벤트에는 재고 차감에 꼭 필요한
 * 상품 코드와 수량만 담습니다.
 */
public record OrderPaid(OrderNumber orderNumber, List<StockChange> stockChanges,
                        Instant occurredAt) implements DomainEvent {

    /** 컨텍스트 간 전달용 최소 정보. OrderLine 자체를 넘기면 내부 모델이 새어 나갑니다. */
    public record StockChange(String productCode, int quantity) {
    }
}

package com.hoonshop.catalog.domain;

import com.hoonshop.common.domain.DomainEvent;
import java.time.Instant;

/** 재고가 0이 된 순간. 품절 알림·재입고 대기 신청 같은 후속 처리의 시작점입니다. */
public record StockDepleted(ProductCode productCode, Instant occurredAt) implements DomainEvent {

    public StockDepleted(ProductCode productCode) {
        this(productCode, Instant.now());
    }
}

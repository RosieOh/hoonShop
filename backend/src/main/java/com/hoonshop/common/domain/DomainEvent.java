package com.hoonshop.common.domain;

import java.time.Instant;

/**
 * 도메인 이벤트 표지 인터페이스.
 *
 * <p>컨텍스트 간 결합을 끊는 통로입니다. 예를 들어 결제가 승인되면 재고를 빼야 하는데,
 * payment가 catalog를 직접 호출하면 두 컨텍스트가 붙어버립니다. 대신 payment는
 * {@code PaymentApproved}를 발행하고, catalog가 그것을 구독합니다.
 */
public interface DomainEvent {

    Instant occurredAt();
}

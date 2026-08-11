package com.hoonshop.common.domain;

import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Transient;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 애그리거트 루트 공통 기반.
 *
 * <p>Spring Data의 {@code AbstractAggregateRoot}를 쓰지 않고 직접 둔 이유는,
 * 도메인 계층이 Spring에 의존하지 않게 하기 위해서입니다 (ArchUnit 테스트로 강제).
 * 대신 이벤트 발행 시점은 애플리케이션 계층이 명시적으로 정합니다 —
 * 저장 시 자동 발행보다 코드를 읽었을 때 언제 나가는지가 분명합니다.
 *
 * <p>참고: 도메인 엔티티가 JPA 애너테이션을 직접 다는 것은 의도한 절충입니다.
 * 순수 도메인 모델 + 별도 영속성 모델 + 매퍼 구성이 이론적으로는 더 깨끗하지만,
 * 이 규모에서는 매퍼 유지비가 이득을 넘어섭니다. 대신 도메인이 Spring과
 * 다른 컨텍스트 내부를 모른다는 원칙은 테스트로 지킵니다.
 */
@MappedSuperclass
public abstract class AggregateRoot {

    @Transient
    private final List<DomainEvent> domainEvents = new ArrayList<>();

    protected void registerEvent(DomainEvent event) {
        domainEvents.add(event);
    }

    public List<DomainEvent> domainEvents() {
        return Collections.unmodifiableList(domainEvents);
    }

    /** 발행 후 비웁니다. 같은 이벤트가 두 번 나가면 재고가 두 번 빠집니다. */
    public List<DomainEvent> pollEvents() {
        List<DomainEvent> polled = List.copyOf(domainEvents);
        domainEvents.clear();
        return polled;
    }
}

package com.hoonshop.order.infrastructure;

import com.hoonshop.common.domain.DomainException;
import com.hoonshop.order.domain.Order;
import com.hoonshop.order.domain.OrderNumber;
import com.hoonshop.order.domain.OrderRepository;
import com.hoonshop.payment.domain.PaymentApproved;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 결제 승인 → 주문 상태 변경.
 *
 * <p>구독자를 {@code infrastructure}에 두는 이유: 다른 컨텍스트의 이벤트 타입을 아는 것은
 * 어댑터의 일입니다. 도메인은 자기 이벤트만 알아야 하고, 그래야 ArchUnit이 강제하는
 * "도메인은 다른 컨텍스트를 모른다" 규칙이 유지됩니다.
 *
 * <p>{@code @TransactionalEventListener(AFTER_COMMIT)}가 아니라 일반
 * {@code @EventListener}인 것이 중요합니다. 커밋 이후로 미루면 결제만 승인되고 주문은
 * 대기 상태로 남는 창이 생깁니다. 지금은 같은 트랜잭션 안에서 함께 커밋되거나 함께 롤백됩니다.
 */
@Component
public class PaymentEventListener {

    private final OrderRepository orders;
    private final ApplicationEventPublisher events;

    public PaymentEventListener(OrderRepository orders, ApplicationEventPublisher events) {
        this.orders = orders;
        this.events = events;
    }

    @EventListener
    public void on(PaymentApproved event) {
        Order order = orders.findByOrderNumber(OrderNumber.of(event.orderNumber()))
                .orElseThrow(() -> new DomainException.NotFound("ORDER_NOT_FOUND",
                        "결제된 주문을 찾을 수 없습니다: " + event.orderNumber()));

        order.markPaid();
        orders.save(order);

        // OrderPaid가 이어서 나가고, catalog가 재고를 뺍니다.
        order.pollEvents().forEach(events::publishEvent);
    }
}

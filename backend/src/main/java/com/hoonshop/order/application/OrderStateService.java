package com.hoonshop.order.application;

import com.hoonshop.common.domain.DomainException;
import com.hoonshop.order.domain.*;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 주문 상태 변경의 트랜잭션 경계.
 *
 * <p>{@link CancelOrderService}가 PG 환불을 트랜잭션 밖에서 호출한 뒤, 결과 반영만
 * 이 서비스에 맡깁니다. 결제 쪽의 {@code PaymentTransactionService}와 같은 이유로
 * 분리되어 있습니다 — 외부 호출과 DB 트랜잭션을 겹치지 않게 하려는 것입니다.
 */
@Service
public class OrderStateService {

    private final OrderRepository orders;
    private final ApplicationEventPublisher events;

    public OrderStateService(OrderRepository orders, ApplicationEventPublisher events) {
        this.orders = orders;
        this.events = events;
    }

    /** 취소 반영 → OrderCancelled 발행 → 재고 복원이 같은 트랜잭션에서 일어납니다. */
    @Transactional
    public OrderView markCancelled(String orderNumber, String reason) {
        Order order = load(orderNumber);
        order.cancel(reason);
        orders.save(order);
        order.pollEvents().forEach(events::publishEvent);
        return OrderView.from(order);
    }

    /** 관리자의 단계 진행 (결제완료 → 제작중 → 발송 → 배송완료). */
    @Transactional
    public OrderView advance(String orderNumber, OrderStatus target) {
        if (target == OrderStatus.CANCELLED) {
            throw new DomainException.Conflict("USE_CANCEL_API",
                    "취소는 환불 처리가 필요하므로 취소 전용 흐름을 사용해야 합니다.");
        }
        Order order = load(orderNumber);
        order.changeStatus(target);
        orders.save(order);
        order.pollEvents().forEach(events::publishEvent);
        return OrderView.from(order);
    }

    private Order load(String orderNumber) {
        return orders.findByOrderNumber(OrderNumber.of(orderNumber))
                .orElseThrow(() -> new DomainException.NotFound("ORDER_NOT_FOUND",
                        "주문 내역이 없습니다: " + orderNumber));
    }
}

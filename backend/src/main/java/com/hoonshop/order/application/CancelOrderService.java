package com.hoonshop.order.application;

import com.hoonshop.common.domain.DomainException;
import com.hoonshop.order.application.port.RefundPort;
import com.hoonshop.order.domain.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 주문 취소.
 *
 * <p>승인과 마찬가지로 <b>이 클래스에는 트랜잭션이 없습니다.</b> PG 환불 호출이 중간에 있어서,
 * 전체를 트랜잭션으로 감싸면 환불 응답을 기다리는 동안 주문 행과 재고 락을 붙잡게 됩니다.
 *
 * <p>순서: <b>환불 먼저, 주문 취소는 그 다음.</b>
 * 반대로 하면 주문은 취소됐는데 환불이 실패한 상태가 남고, 고객은 상품도 돈도 없게 됩니다.
 * 환불이 성공한 뒤 주문 취소가 실패하는 경우는 결제 원장에 취소 기록이 남아 복구할 수 있지만,
 * 그 반대는 추적할 단서가 없습니다.
 */
@Service
public class CancelOrderService {

    private static final Logger log = LoggerFactory.getLogger(CancelOrderService.class);

    private final OrderRepository orders;
    private final RefundPort refundPort;
    private final OrderStateService stateService;

    public CancelOrderService(OrderRepository orders, RefundPort refundPort,
                              OrderStateService stateService) {
        this.orders = orders;
        this.refundPort = refundPort;
        this.stateService = stateService;
    }

    public OrderView cancel(String orderNumber, String reason, String requesterEmail,
                            boolean isAdmin) {
        Order order = loadForCancel(orderNumber, requesterEmail, isAdmin);

        // 1) 환불 (트랜잭션 밖 — 외부 호출)
        long refunded = refundPort.refundIfPaid(orderNumber, reason);

        // 2) 주문 취소 → OrderCancelled → 재고 복원 (한 트랜잭션)
        OrderView cancelled = stateService.markCancelled(orderNumber, reason);
        log.info("주문 취소 완료 — order={} 환불={}원", orderNumber, refunded);
        return cancelled;
    }

    @Transactional(readOnly = true)
    public Order loadForCancel(String orderNumber, String requesterEmail, boolean isAdmin) {
        Order order = orders.findByOrderNumber(OrderNumber.of(orderNumber))
                .orElseThrow(() -> new DomainException.NotFound("ORDER_NOT_FOUND",
                        "주문 내역이 없습니다: " + orderNumber));

        // 고객은 자기 주문만 취소할 수 있습니다.
        if (!isAdmin && !order.isOwnedBy(requesterEmail)) {
            throw new DomainException.NotFound("ORDER_NOT_FOUND", "주문 내역이 없습니다.");
        }
        if (order.status().isTerminal()) {
            throw new DomainException.Conflict("ALREADY_CLOSED",
                    "이미 종료된 주문은 취소할 수 없습니다.");
        }
        // 고객은 발송 이후 취소 불가 — 이미 물건이 나갔습니다.
        if (!isAdmin && order.status() == OrderStatus.SHIPPED) {
            throw new DomainException.Conflict("ALREADY_SHIPPED",
                    "이미 발송된 주문입니다. 반품으로 진행해 주세요.");
        }
        return order;
    }
}

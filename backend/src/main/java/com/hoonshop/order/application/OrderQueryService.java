package com.hoonshop.order.application;

import com.hoonshop.common.domain.DomainException;
import com.hoonshop.order.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class OrderQueryService {

    private final OrderRepository orders;

    public OrderQueryService(OrderRepository orders) {
        this.orders = orders;
    }

    public List<OrderView> myOrders(String email) {
        return orders.findByCustomerEmail(email).stream().map(OrderView::from).toList();
    }

    /**
     * 주문 단건 조회.
     *
     * <p>이메일을 함께 받아 소유자를 확인합니다. 주문번호만으로 조회하게 두면
     * 번호를 바꿔가며 남의 주문을 볼 수 있습니다 (IDOR).
     */
    public OrderView myOrder(String orderNumber, String email) {
        Order order = load(orderNumber);
        if (!order.isOwnedBy(email)) {
            throw new DomainException.NotFound("ORDER_NOT_FOUND", "주문 내역이 없습니다.");
        }
        return OrderView.from(order);
    }

    public List<OrderView> searchForAdmin(String status, String keyword) {
        OrderStatus parsed = (status == null || status.isBlank() || "ALL".equalsIgnoreCase(status))
                ? null
                : OrderStatus.valueOf(status);
        return orders.searchForAdmin(parsed, keyword).stream().map(OrderView::from).toList();
    }

    Order load(String orderNumber) {
        return orders.findByOrderNumber(OrderNumber.of(orderNumber))
                .orElseThrow(() -> new DomainException.NotFound("ORDER_NOT_FOUND",
                        "주문 내역이 없습니다: " + orderNumber));
    }
}

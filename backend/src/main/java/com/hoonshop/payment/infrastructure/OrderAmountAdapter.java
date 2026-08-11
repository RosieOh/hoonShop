package com.hoonshop.payment.infrastructure;

import com.hoonshop.common.domain.DomainException;
import com.hoonshop.common.domain.Money;
import com.hoonshop.order.domain.Order;
import com.hoonshop.order.domain.OrderNumber;
import com.hoonshop.order.domain.OrderRepository;
import com.hoonshop.order.domain.OrderStatus;
import com.hoonshop.payment.application.port.OrderAmountPort;
import org.springframework.stereotype.Component;

@Component
public class OrderAmountAdapter implements OrderAmountPort {

    private final OrderRepository orders;

    public OrderAmountAdapter(OrderRepository orders) {
        this.orders = orders;
    }

    @Override
    public Money payableOf(String orderNumber, String customerEmail) {
        Order order = orders.findByOrderNumber(OrderNumber.of(orderNumber))
                .orElseThrow(() -> new DomainException.NotFound("ORDER_NOT_FOUND",
                        "주문 내역이 없습니다: " + orderNumber));

        // 남의 주문번호로 결제를 걸어 상태를 바꾸지 못하게 소유자를 확인합니다.
        if (!order.isOwnedBy(customerEmail)) {
            throw new DomainException.NotFound("ORDER_NOT_FOUND", "주문 내역이 없습니다.");
        }
        if (order.status() != OrderStatus.PAYMENT_PENDING) {
            throw new DomainException.Conflict("ALREADY_PAID",
                    "이미 결제가 완료되었거나 취소된 주문입니다.");
        }

        return order.amounts().payable();
    }
}

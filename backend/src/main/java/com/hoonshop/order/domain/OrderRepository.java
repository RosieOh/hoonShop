package com.hoonshop.order.domain;

import java.util.List;
import java.util.Optional;

public interface OrderRepository {

    Optional<Order> findByOrderNumber(OrderNumber orderNumber);

    List<Order> findByCustomerEmail(String email);

    List<Order> searchForAdmin(OrderStatus status, String keyword);

    List<Order> findAllValid();

    Order save(Order order);

    /** 주문번호 채번용 시퀀스. */
    long nextSequence();
}

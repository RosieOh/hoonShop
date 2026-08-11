package com.hoonshop.order.infrastructure;

import com.hoonshop.order.domain.Order;
import com.hoonshop.order.domain.OrderNumber;
import com.hoonshop.order.domain.OrderRepository;
import com.hoonshop.order.domain.OrderStatus;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class OrderRepositoryAdapter implements OrderRepository {

    private final OrderJpaRepository jpa;

    public OrderRepositoryAdapter(OrderJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<Order> findByOrderNumber(OrderNumber orderNumber) {
        return jpa.findByOrderNumberValue(orderNumber.value());
    }

    @Override
    public List<Order> findByCustomerEmail(String email) {
        return jpa.findByCustomerEmailOrderByCreatedAtDesc(email);
    }

    @Override
    public List<Order> searchForAdmin(OrderStatus status, String keyword) {
        String pattern = (keyword == null || keyword.isBlank())
                ? null
                : "%" + keyword.trim().toLowerCase() + "%";
        return jpa.search(status, pattern);
    }

    @Override
    public List<Order> findAllValid() {
        return jpa.findByStatusNotOrderByCreatedAtDesc(OrderStatus.CANCELLED);
    }

    @Override
    public Order save(Order order) {
        return jpa.save(order);
    }

    @Override
    public long nextSequence() {
        return jpa.nextOrderSequence();
    }
}

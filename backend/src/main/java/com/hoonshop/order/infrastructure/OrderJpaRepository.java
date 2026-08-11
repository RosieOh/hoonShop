package com.hoonshop.order.infrastructure;

import com.hoonshop.order.domain.Order;
import com.hoonshop.order.domain.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OrderJpaRepository extends JpaRepository<Order, Long> {

    Optional<Order> findByOrderNumberValue(String orderNumber);

    List<Order> findByCustomerEmailOrderByCreatedAtDesc(String email);

    List<Order> findByStatusNotOrderByCreatedAtDesc(OrderStatus status);

    /**
     * 관리자 목록. 상태·키워드가 null이면 조건에서 빠집니다.
     *
     * <p>Specification 대신 JPQL을 쓴 이유는 조건이 두 개뿐이라 명세 조립이 오히려 장황해서입니다.
     * 조건이 늘면 Specification으로 옮기세요.
     */
    @Query("""
            select o from Order o
            where (:status is null or o.status = :status)
              and (:keyword is null
                   or lower(o.orderNumber.value) like :keyword
                   or lower(o.customerName) like :keyword
                   or lower(o.customerEmail) like :keyword)
            order by o.createdAt desc
            """)
    List<Order> search(@Param("status") OrderStatus status, @Param("keyword") String keyword);

    @Query(value = "select nextval('order_number_seq')", nativeQuery = true)
    Long nextOrderSequence();
}

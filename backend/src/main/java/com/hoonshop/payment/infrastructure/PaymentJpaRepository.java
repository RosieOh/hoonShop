package com.hoonshop.payment.infrastructure;

import com.hoonshop.payment.domain.Payment;
import com.hoonshop.payment.domain.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface PaymentJpaRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByIdempotencyKey(String idempotencyKey);

    Optional<Payment> findByPaymentKey(String paymentKey);

    Optional<Payment> findByOrderNumber(String orderNumber);

    @Query("""
            select p from Payment p
            where p.status in :statuses
              and p.requestedAt < :olderThan
            order by p.requestedAt asc
            """)
    List<Payment> findByStatusInAndRequestedAtBefore(@Param("statuses") List<PaymentStatus> statuses,
                                                     @Param("olderThan") Instant olderThan);
}

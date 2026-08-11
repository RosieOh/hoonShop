package com.hoonshop.payment.infrastructure;

import com.hoonshop.payment.domain.Payment;
import com.hoonshop.payment.domain.PaymentRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class PaymentRepositoryAdapter implements PaymentRepository {

    private final PaymentJpaRepository jpa;

    public PaymentRepositoryAdapter(PaymentJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<Payment> findByIdempotencyKey(String idempotencyKey) {
        return jpa.findByIdempotencyKey(idempotencyKey);
    }

    @Override
    public Optional<Payment> findByOrderNumber(String orderNumber) {
        return jpa.findByOrderNumber(orderNumber);
    }

    @Override
    public Payment save(Payment payment) {
        return jpa.save(payment);
    }
}

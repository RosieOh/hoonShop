package com.hoonshop.payment.infrastructure;

import com.hoonshop.payment.domain.Payment;
import com.hoonshop.payment.domain.PaymentRepository;
import com.hoonshop.payment.domain.PaymentStatus;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public class PaymentRepositoryAdapter implements PaymentRepository {

    private final PaymentJpaRepository jpa;

    public PaymentRepositoryAdapter(PaymentJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Payment save(Payment payment) {
        return jpa.save(payment);
    }

    @Override
    public Optional<Payment> findById(Long id) {
        return jpa.findById(id);
    }

    @Override
    public Optional<Payment> findByIdempotencyKey(String idempotencyKey) {
        return jpa.findByIdempotencyKey(idempotencyKey);
    }

    @Override
    public Optional<Payment> findByPaymentKey(String paymentKey) {
        return jpa.findByPaymentKey(paymentKey);
    }

    @Override
    public Optional<Payment> findByOrderNumber(String orderNumber) {
        return jpa.findByOrderNumber(orderNumber);
    }

    @Override
    public List<Payment> findPendingReconciliation(Instant olderThan) {
        return jpa.findByStatusInAndRequestedAtBefore(
                List.of(PaymentStatus.UNKNOWN, PaymentStatus.REQUESTED), olderThan);
    }
}

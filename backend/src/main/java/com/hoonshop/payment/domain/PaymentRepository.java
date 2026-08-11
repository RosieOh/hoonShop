package com.hoonshop.payment.domain;

import java.util.Optional;

public interface PaymentRepository {

    Optional<Payment> findByIdempotencyKey(String idempotencyKey);

    Optional<Payment> findByOrderNumber(String orderNumber);

    Payment save(Payment payment);
}

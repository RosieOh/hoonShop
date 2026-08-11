package com.hoonshop.payment.domain;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository {

    Payment save(Payment payment);

    Optional<Payment> findById(Long id);

    Optional<Payment> findByIdempotencyKey(String idempotencyKey);

    Optional<Payment> findByPaymentKey(String paymentKey);

    Optional<Payment> findByOrderNumber(String orderNumber);

    /**
     * 대사 대상 조회.
     *
     * <p>결과를 모르는 결제(UNKNOWN)와, 승인 요청만 기록되고 진행이 멈춘 결제(REQUESTED)를
     * 찾습니다. 후자는 PG 호출 도중 서버가 죽은 경우로, 실제로는 승인됐을 수 있습니다.
     */
    List<Payment> findPendingReconciliation(Instant olderThan);
}

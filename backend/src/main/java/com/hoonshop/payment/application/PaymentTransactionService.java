package com.hoonshop.payment.application;

import com.hoonshop.common.domain.DomainException;
import com.hoonshop.common.domain.Money;
import com.hoonshop.payment.application.port.OrderAmountPort;
import com.hoonshop.payment.domain.*;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 결제의 트랜잭션 경계.
 *
 * <p>이 클래스가 따로 존재하는 이유는 <b>PG 호출을 트랜잭션 밖으로 빼기 위해서</b>입니다.
 *
 * <p>승인 로직 전체를 하나의 {@code @Transactional}로 감싸면, PG 응답을 기다리는
 * 수 초 동안 DB 커넥션과 (재고 차감의) 비관적 락을 붙잡고 있게 됩니다. PG가 느려지는
 * 순간 커넥션 풀이 마르고, 결제와 무관한 상품 조회까지 전부 멈춥니다.
 * 결제 장애가 사이트 전체 장애로 번지는 가장 흔한 경로입니다.
 *
 * <p>그래서 흐름을 셋으로 쪼갭니다.
 * <ol>
 *   <li>{@link #beginOrGetExisting} — 짧은 트랜잭션. 시도 기록을 남기고 <b>커밋</b>합니다.</li>
 *   <li>PG 호출 — 트랜잭션 없음 ({@link ConfirmPaymentService}가 담당)</li>
 *   <li>{@link #settleApproved} 등 — 결과 반영. 여기서 주문·재고가 함께 커밋됩니다.</li>
 * </ol>
 *
 * <p>{@code REQUIRES_NEW}를 쓰는 이유: 실패 기록만큼은 반드시 남아야 합니다.
 * 바깥 트랜잭션이 롤백되어도 "시도했다"는 사실과 "결과를 모른다"는 사실은 지워지면 안 됩니다.
 */
@Service
public class PaymentTransactionService {

    private final PaymentRepository payments;
    private final OrderAmountPort orderAmounts;
    private final ApplicationEventPublisher events;

    public PaymentTransactionService(PaymentRepository payments, OrderAmountPort orderAmounts,
                                     ApplicationEventPublisher events) {
        this.payments = payments;
        this.orderAmounts = orderAmounts;
        this.events = events;
    }

    /**
     * 멱등키를 확인하고, 처음 보는 요청이면 승인 시도를 기록합니다.
     *
     * <p>PG를 부르기 <b>전에</b> 커밋되는 것이 핵심입니다. 호출 도중 서버가 죽어도
     * REQUESTED 상태의 레코드가 남아 대사 대상이 됩니다. 호출 후에 기록하면
     * 그 사이의 장애는 아무 흔적도 남기지 않습니다.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Payment beginOrGetExisting(String idempotencyKey, String orderNumber,
                                      String customerEmail) {
        return payments.findByIdempotencyKey(idempotencyKey)
                .orElseGet(() -> {
                    // 청구 금액은 주문에서 읽습니다. 요청 본문의 금액은 쓰지 않습니다.
                    Money payable = orderAmounts.payableOf(orderNumber, customerEmail);
                    return payments.save(
                            Payment.request(idempotencyKey, orderNumber, customerEmail, payable));
                });
    }

    /**
     * 승인 결과 반영 → 주문 결제 완료 → 재고 차감이 <b>한 트랜잭션</b>에서 일어납니다.
     *
     * <p>여기는 묶여 있어야 합니다. 재고 차감만 따로 떼면 "결제는 됐는데 재고는 안 빠진"
     * 상태가 생기고, 되돌리려면 보상 트랜잭션이 필요해집니다.
     */
    @Transactional
    public Payment settleApproved(Long paymentId, PaymentGateway.Approval approval) {
        Payment payment = load(paymentId);
        payment.approve(approval);
        payments.save(payment);
        payment.pollEvents().forEach(events::publishEvent);
        return payment;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Payment settleFailed(Long paymentId, String code, String message) {
        Payment payment = load(paymentId);
        payment.fail(code, message);
        return payments.save(payment);
    }

    /**
     * 결과 미확인 기록.
     *
     * <p>이 메서드가 실패하면 정말로 추적 불가능한 결제가 생기므로, 바깥 트랜잭션과
     * 독립적으로 반드시 커밋되어야 합니다.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Payment settleUnknown(Long paymentId, String reason) {
        Payment payment = load(paymentId);
        payment.markUnknown(reason);
        payments.save(payment);
        payment.pollEvents().forEach(events::publishEvent);
        return payment;
    }

    /** 가상계좌 입금 확인 (웹훅). 여기서 비로소 주문이 결제 완료가 됩니다. */
    @Transactional
    public Payment confirmDeposit(String paymentKey, Money depositedAmount) {
        Payment payment = payments.findByPaymentKey(paymentKey)
                .orElseThrow(() -> new DomainException.NotFound("PAYMENT_NOT_FOUND",
                        "결제 정보를 찾을 수 없습니다."));
        payment.confirmDeposit(depositedAmount);
        payments.save(payment);
        payment.pollEvents().forEach(events::publishEvent);
        return payment;
    }

    @Transactional
    public Payment applyCancellation(Long paymentId, Money amount, String reason) {
        Payment payment = load(paymentId);
        payment.cancel(amount, reason);
        payments.save(payment);
        payment.pollEvents().forEach(events::publishEvent);
        return payment;
    }

    @Transactional
    public Payment resolveByReconciliation(Long paymentId, PaymentGateway.Approval approval) {
        Payment payment = load(paymentId);
        payment.resolveByReconciliation(approval);
        payments.save(payment);
        payment.pollEvents().forEach(events::publishEvent);
        return payment;
    }

    @Transactional
    public Payment resolveAsFailed(Long paymentId, String reason) {
        Payment payment = load(paymentId);
        payment.resolveAsFailed(reason);
        return payments.save(payment);
    }

    private Payment load(Long paymentId) {
        return payments.findById(paymentId)
                .orElseThrow(() -> new DomainException.NotFound("PAYMENT_NOT_FOUND",
                        "결제 정보를 찾을 수 없습니다: " + paymentId));
    }
}

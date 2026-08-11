package com.hoonshop.payment.application;

import com.hoonshop.payment.domain.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * 결제 대사(reconciliation).
 *
 * <p>결제 시스템에서 가장 자주 빠뜨리는 부분입니다. 승인 요청을 보낸 뒤 응답을 못 받은
 * 결제는 우리 DB에서는 "모름"이지만 PG에서는 승인됐을 수 있습니다. 그대로 두면
 * <b>고객 돈은 빠져나갔는데 주문은 없는</b> 상태가 영원히 남습니다.
 *
 * <p>이 서비스가 주기적으로 PG에 다시 물어봐서 상태를 확정합니다.
 * <ul>
 *   <li>PG에 승인 기록이 있음 → 우리 쪽도 승인 처리 (주문·재고가 이어서 처리됨)</li>
 *   <li>PG에 기록이 없음 → 실패로 확정</li>
 * </ul>
 *
 * <p>대상에 {@code REQUESTED}도 포함하는 이유: PG를 부르기 직전에 서버가 죽으면
 * 요청 기록만 남고 멈춥니다. 이것도 실제로 승인됐을 수 있으므로 확인해야 합니다.
 */
@Service
public class PaymentReconciliationService {

    private static final Logger log = LoggerFactory.getLogger(PaymentReconciliationService.class);

    private final PaymentRepository payments;
    private final PaymentTransactionService tx;
    private final PaymentGateway gateway;
    private final Duration graceperiod;

    public PaymentReconciliationService(PaymentRepository payments, PaymentTransactionService tx,
                                        PaymentGateway gateway,
                                        @Value("${hoonshop.payment.reconcile-after:PT5M}")
                                        Duration gracePeriod) {
        this.payments = payments;
        this.tx = tx;
        this.gateway = gateway;
        this.graceperiod = gracePeriod;
    }

    /**
     * 5분마다 미확정 결제를 확인합니다.
     *
     * <p>유예 기간을 두는 이유: 지금 막 진행 중인 결제까지 건드리면 정상 흐름과 충돌합니다.
     */
    @Scheduled(fixedDelayString = "${hoonshop.payment.reconcile-interval-ms:300000}")
    public void reconcile() {
        List<Payment> targets = findTargets();
        if (targets.isEmpty()) {
            return;
        }
        log.warn("결제 대사 시작 — 미확정 {}건", targets.size());
        targets.forEach(this::reconcileOne);
    }

    @Transactional(readOnly = true)
    public List<Payment> findTargets() {
        return payments.findPendingReconciliation(Instant.now().minus(graceperiod));
    }

    private void reconcileOne(Payment payment) {
        // paymentKey가 없으면 PG에 물어볼 방법이 없습니다 — 결제창 단계에서 이탈한 건입니다.
        if (payment.paymentKey() == null) {
            if (payment.status() == PaymentStatus.REQUESTED) {
                tx.resolveAsFailed(payment.id(), "결제창에서 이탈 (paymentKey 없음)");
            }
            return;
        }

        try {
            PaymentGateway.Approval approval = gateway.inquire(payment.paymentKey());

            if (payment.status() == PaymentStatus.UNKNOWN) {
                tx.resolveByReconciliation(payment.id(), approval);
            } else {
                tx.settleApproved(payment.id(), approval);
            }
            log.warn("대사 완료 — 승인으로 확정. order={} amount={}",
                    payment.orderNumber(), approval.approvedAmount());

        } catch (GatewayException e) {
            tx.resolveAsFailed(payment.id(), "PG 조회 결과 승인 기록 없음: " + e.getMessage());
            log.warn("대사 완료 — 실패로 확정. order={}", payment.orderNumber());

        } catch (GatewayTimeoutException e) {
            // 다음 주기에 다시 시도합니다. 상태는 그대로 둡니다.
            log.error("대사 중 PG 응답 없음 — 다음 주기 재시도. order={}", payment.orderNumber());
        }
    }
}

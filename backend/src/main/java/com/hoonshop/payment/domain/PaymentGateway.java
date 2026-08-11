package com.hoonshop.payment.domain;

import com.hoonshop.common.domain.Money;

/**
 * PG사 연동 포트.
 *
 * <p><b>카드번호가 없습니다.</b> 이게 이 인터페이스에서 가장 중요한 점입니다.
 * 카드 정보는 PG가 제공하는 결제창(SDK)에서 직접 입력받고, 우리 서버는 그 결과로 받은
 * {@code paymentKey}만 씁니다. 카드번호가 서버 메모리·로그·DB 어디에도 닿지 않아야
 * PCI-DSS 범위 밖에 있을 수 있습니다. 한 번이라도 서버를 거치면 그 순간부터
 * 로그 마스킹, 저장 암호화, 정기 감사가 전부 우리 책임이 됩니다.
 *
 * <p>구현체는 이 규칙을 반드시 지켜야 합니다:
 * <ul>
 *   <li>타임아웃을 반드시 설정한다 — 무한 대기는 커넥션 풀을 말립니다</li>
 *   <li>결과를 확신할 수 없으면 {@link GatewayTimeoutException}을 던진다.
 *       실패로 뭉뚱그리면 이미 승인된 결제를 놓칩니다</li>
 *   <li>승인 응답의 금액을 그대로 돌려준다 — 호출자가 교차 검증합니다</li>
 * </ul>
 */
public interface PaymentGateway {

    /**
     * 결제 승인.
     *
     * @throws GatewayTimeoutException 결과를 알 수 없을 때 (타임아웃·네트워크 단절)
     * @throws GatewayException        PG가 명시적으로 거절했을 때
     */
    Approval confirm(ConfirmCommand command);

    /**
     * 결제 재조회. 대사(reconciliation)에서 "결과를 모르는" 결제를 확정할 때 씁니다.
     */
    Approval inquire(String paymentKey);

    /**
     * 승인 취소. {@code cancelAmount}가 전체 금액보다 작으면 부분 취소입니다.
     *
     * @param idempotencyKey 취소도 재시도될 수 있으므로 멱등키가 필요합니다
     */
    Cancellation cancel(String paymentKey, Money cancelAmount, String reason,
                        String idempotencyKey);

    /** 웹훅 서명 검증. 검증 없이 웹훅을 신뢰하면 누구나 "입금됐다"고 우길 수 있습니다. */
    boolean verifyWebhookSignature(String payload, String signature);

    /**
     * @param paymentKey PG 결제창이 발급한 키. 카드 정보가 아닙니다.
     * @param amount     서버가 계산한 청구 금액. PG가 이 값과 다르면 승인이 거절됩니다.
     */
    record ConfirmCommand(String orderNumber, String paymentKey, Money amount) {
    }

    /**
     * @param approvedAmount PG가 실제로 승인한 금액. 요청 금액과 반드시 대조해야 합니다.
     */
    record Approval(String paymentKey, PaymentMethod method, Money approvedAmount,
                    PaymentStatus status, java.time.Instant approvedAt, String receiptUrl,
                    VirtualAccount virtualAccount) {

        public boolean isWaitingForDeposit() {
            return status == PaymentStatus.WAITING_FOR_DEPOSIT;
        }
    }

    /** 가상계좌 정보. 입금은 나중에 웹훅으로 통보됩니다. */
    record VirtualAccount(String bank, String accountNumber, String holderName,
                          java.time.Instant dueDate) {
    }

    record Cancellation(String paymentKey, Money cancelledAmount, Money remainingAmount,
                        java.time.Instant cancelledAt) {
    }
}

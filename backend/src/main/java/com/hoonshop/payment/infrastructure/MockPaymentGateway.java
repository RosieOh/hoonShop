package com.hoonshop.payment.infrastructure;

import com.hoonshop.common.domain.Money;
import com.hoonshop.payment.domain.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 개발용 목 PG. {@code hoonshop.payment.provider=mock}(기본값)일 때 활성화됩니다.
 *
 * <p><b>실패 경로를 개발 중에 반복해서 밟아볼 수 없으면, 그 경로는 배포 후 처음 실행됩니다.</b>
 * 그래서 정상 승인뿐 아니라 거절·타임아웃·가상계좌까지 모두 재현할 수 있게 했습니다.
 * paymentKey 접두사로 시나리오를 고릅니다.
 *
 * <table>
 *   <tr><th>paymentKey 접두사</th><th>결과</th></tr>
 *   <tr><td>{@code DECLINE_}</td><td>카드 승인 거절 (확정된 실패)</td></tr>
 *   <tr><td>{@code TIMEOUT_}</td><td>응답 없음 → UNKNOWN → 대사 대상</td></tr>
 *   <tr><td>{@code VBANK_}</td><td>가상계좌 발급 (입금 대기)</td></tr>
 *   <tr><td>{@code WRONG_AMOUNT_}</td><td>요청과 다른 금액으로 승인 (교차검증 테스트)</td></tr>
 *   <tr><td>그 외</td><td>정상 승인</td></tr>
 * </table>
 */
@Component
@ConditionalOnProperty(name = "hoonshop.payment.provider", havingValue = "mock",
        matchIfMissing = true)
public class MockPaymentGateway implements PaymentGateway {

    /** 대사 테스트를 위해 "PG 쪽 승인 기록"을 흉내 냅니다. */
    private final Map<String, Approval> approvals = new ConcurrentHashMap<>();

    @Override
    public Approval confirm(ConfirmCommand command) {
        String key = command.paymentKey() == null ? "" : command.paymentKey();

        if (key.startsWith("DECLINE_")) {
            throw new GatewayException("CARD_DECLINED",
                    "카드사 승인이 거절되었습니다. 다른 결제수단을 이용해 주세요.");
        }

        if (key.startsWith("TIMEOUT_")) {
            // 실제로는 승인됐다고 가정합니다 — 대사가 이걸 찾아내야 정상입니다.
            Approval hidden = approved(command, command.amount());
            approvals.put(hidden.paymentKey(), hidden);
            throw new GatewayTimeoutException("PG 응답 시간 초과 (목)", null);
        }

        if (key.startsWith("VBANK_")) {
            Approval va = new Approval(issueKey(), PaymentMethod.VIRTUAL, command.amount(),
                    PaymentStatus.WAITING_FOR_DEPOSIT, Instant.now(), null,
                    new VirtualAccount("신한", "110-123-456789", "훈샵",
                            Instant.now().plus(3, ChronoUnit.DAYS)));
            approvals.put(va.paymentKey(), va);
            return va;
        }

        if (key.startsWith("WRONG_AMOUNT_")) {
            Approval wrong = approved(command, command.amount().plus(Money.won(1_000)));
            approvals.put(wrong.paymentKey(), wrong);
            return wrong;
        }

        Approval ok = approved(command, command.amount());
        approvals.put(ok.paymentKey(), ok);
        return ok;
    }

    @Override
    public Approval inquire(String paymentKey) {
        Approval found = approvals.get(paymentKey);
        if (found == null) {
            throw new GatewayException("NOT_APPROVED", "PG에 승인 기록이 없습니다.");
        }
        return found;
    }

    @Override
    public Cancellation cancel(String paymentKey, Money cancelAmount, String reason,
                               String idempotencyKey) {
        Approval approval = approvals.get(paymentKey);
        Money total = approval == null ? cancelAmount : approval.approvedAmount();
        return new Cancellation(paymentKey, cancelAmount,
                total.minusOrZero(cancelAmount), Instant.now());
    }

    /** 목에서는 서명 검증을 통과시킵니다. 실연동에서는 반드시 HMAC을 검증해야 합니다. */
    @Override
    public boolean verifyWebhookSignature(String payload, String signature) {
        return true;
    }

    /** 목의 가상계좌 입금을 테스트에서 흉내 내기 위한 헬퍼 */
    public void simulateDeposit(String paymentKey) {
        Approval waiting = approvals.get(paymentKey);
        if (waiting != null) {
            approvals.put(paymentKey, new Approval(paymentKey, waiting.method(),
                    waiting.approvedAmount(), PaymentStatus.APPROVED, Instant.now(), null, null));
        }
    }

    private Approval approved(ConfirmCommand command, Money amount) {
        return new Approval(issueKey(), PaymentMethod.CARD, amount, PaymentStatus.APPROVED,
                Instant.now(), "https://example.com/receipt/" + command.orderNumber(), null);
    }

    private String issueKey() {
        return "PAY_" + UUID.randomUUID().toString().replace("-", "")
                .substring(0, 12).toUpperCase();
    }
}

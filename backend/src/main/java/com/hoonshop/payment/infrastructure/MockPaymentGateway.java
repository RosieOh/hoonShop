package com.hoonshop.payment.infrastructure;

import com.hoonshop.payment.domain.PaymentGateway;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * 목 PG.
 *
 * <p>실연동 전까지 결제 흐름 전체를 돌려보기 위한 구현입니다.
 * 카드번호 끝 4자리가 {@code 0000}이면 승인 거절을 재현합니다 — 실패 경로를 개발 중에
 * 반복해서 밟아볼 수 없으면, 그 경로는 배포 후 처음 실행됩니다.
 *
 * <p>실연동 시 이 클래스를 지우고 {@code TossPaymentsGateway}를 만들면 됩니다.
 * 도메인·애플리케이션 코드는 한 줄도 바뀌지 않습니다.
 */
@Component
public class MockPaymentGateway implements PaymentGateway {

    @Override
    public ApprovalResult approve(ApprovalRequest request) {
        String digits = request.cardNumberForDemo() == null
                ? ""
                : request.cardNumberForDemo().replaceAll("\\D", "");

        if (digits.endsWith("0000")) {
            return ApprovalResult.declined("CARD_DECLINED",
                    "카드사 승인이 거절되었습니다. 다른 결제수단을 이용해 주세요.");
        }

        return ApprovalResult.success(
                "PAY_" + UUID.randomUUID().toString().replace("-", "").substring(0, 10)
                        .toUpperCase());
    }
}

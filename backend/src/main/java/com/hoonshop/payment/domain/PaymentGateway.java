package com.hoonshop.payment.domain;

import com.hoonshop.common.domain.Money;

/**
 * PG사 연동 포트.
 *
 * <p>토스페이먼츠·포트원 어느 쪽을 붙이든 도메인은 이 인터페이스만 압니다.
 * 지금은 목 구현이 들어가 있고, 실연동 시 이 인터페이스를 구현하는 클래스 하나만 추가하면 됩니다.
 *
 * <p>실제 연동에서는 프론트가 PG 결제창에서 받은 {@code paymentKey}를 서버로 넘기고,
 * 서버가 <b>금액을 검증한 뒤</b> 승인 API를 호출합니다. 카드번호는 서버에 오지 않습니다.
 */
public interface PaymentGateway {

    ApprovalResult approve(ApprovalRequest request);

    record ApprovalRequest(String orderNumber, PaymentMethod method, Money amount,
                           String paymentKeyFromClient, String cardNumberForDemo) {
    }

    record ApprovalResult(boolean approved, String paymentKey, String failureCode,
                          String failureMessage) {

        public static ApprovalResult success(String paymentKey) {
            return new ApprovalResult(true, paymentKey, null, null);
        }

        public static ApprovalResult declined(String code, String message) {
            return new ApprovalResult(false, null, code, message);
        }
    }
}

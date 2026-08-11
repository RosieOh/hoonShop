package com.hoonshop.payment.application.port;

import com.hoonshop.common.domain.Money;

/**
 * 결제 → 주문 방향의 조회 포트.
 *
 * <p>결제 컨텍스트가 주문에 대해 알아야 하는 것은 "이 주문의 청구 금액이 얼마이고,
 * 요청자가 주인이 맞는가"뿐입니다. 주문 상태를 바꾸는 일은 결제가 하지 않고,
 * {@code PaymentApproved} 이벤트를 받은 주문 컨텍스트가 스스로 합니다.
 */
public interface OrderAmountPort {

    /**
     * @throws com.hoonshop.common.domain.DomainException.NotFound 주문이 없거나 요청자의 것이 아닐 때
     * @throws com.hoonshop.common.domain.DomainException.Conflict 이미 결제된 주문일 때
     */
    Money payableOf(String orderNumber, String customerEmail);
}

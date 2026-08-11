package com.hoonshop.order.application.port;

/**
 * 주문 → 결제 방향의 환불 포트.
 *
 * <p><b>취소의 주도권은 주문에 있습니다.</b> 결제 취소가 주문을 취소시키고 주문 취소가
 * 결제를 취소시키면 순환이 생겨, 이벤트가 서로를 무한히 부르거나 한쪽만 취소되는 상태가
 * 만들어집니다. 방향을 한쪽으로 고정했습니다: 주문이 취소를 결정하고 결제에 환불을 지시합니다.
 */
public interface RefundPort {

    /**
     * 결제가 있으면 환불하고, 없으면(결제 전 취소) 아무것도 하지 않습니다.
     *
     * @return 실제로 환불한 금액. 결제가 없었으면 0.
     */
    long refundIfPaid(String orderNumber, String reason);
}

package com.hoonshop.order.domain;

import com.hoonshop.common.domain.DomainException;

import java.util.Set;

/**
 * 주문 상태 — 상태 기계.
 *
 * <p>허용 전이를 enum 안에 두는 이유: 서비스 코드에 {@code if (status == PAID) ...}로 흩어놓으면
 * 새 상태를 추가할 때 빠뜨리는 분기가 반드시 생깁니다. 여기 한 곳만 보면 전체 흐름을 알 수 있어야 합니다.
 */
public enum OrderStatus {
    /** 주문서는 만들어졌지만 아직 결제 승인 전 */
    PAYMENT_PENDING("결제 대기"),
    PAID("결제 완료"),
    MAKING("제작 중"),
    SHIPPED("발송"),
    DELIVERED("배송 완료"),
    CANCELLED("취소");

    private final String label;

    OrderStatus(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    private Set<OrderStatus> allowedNext() {
        return switch (this) {
            case PAYMENT_PENDING -> Set.of(PAID, CANCELLED);
            case PAID -> Set.of(MAKING, CANCELLED);
            case MAKING -> Set.of(SHIPPED, CANCELLED);
            case SHIPPED -> Set.of(DELIVERED);
            case DELIVERED, CANCELLED -> Set.of();
        };
    }

    /** 관리자가 "다음 단계" 버튼을 눌렀을 때 갈 곳. 종료 상태면 비어 있습니다. */
    public OrderStatus next() {
        return switch (this) {
            case PAYMENT_PENDING -> PAID;
            case PAID -> MAKING;
            case MAKING -> SHIPPED;
            case SHIPPED -> DELIVERED;
            case DELIVERED, CANCELLED -> null;
        };
    }

    public boolean isTerminal() {
        return this == DELIVERED || this == CANCELLED;
    }

    public void ensureCanTransitionTo(OrderStatus target) {
        if (!allowedNext().contains(target)) {
            throw new DomainException.Conflict("INVALID_STATUS_TRANSITION",
                    "%s 상태에서 %s(으)로 바꿀 수 없습니다.".formatted(label, target.label));
        }
    }
}

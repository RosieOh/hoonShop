package com.hoonshop.payment.domain;

import com.hoonshop.common.domain.DomainEvent;

import java.time.Instant;

/**
 * 결제 결과를 확정하지 못함 — 사람이 봐야 하는 사건.
 *
 * <p>이 이벤트는 조용히 로그만 남기고 끝내면 안 됩니다. 고객 돈이 걸린 문제이므로
 * 실서비스에서는 즉시 알림(슬랙·이메일)으로 연결해야 합니다.
 */
public record PaymentNeedsReconciliation(String orderNumber, String idempotencyKey, String reason,
                                         Instant occurredAt) implements DomainEvent {
}

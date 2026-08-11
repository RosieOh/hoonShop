package com.hoonshop.payment.domain;

/**
 * 결제 상태.
 *
 * <p>{@link #UNKNOWN}이 이 열거형에서 가장 중요합니다. 대부분의 결제 구현이 성공과 실패만
 * 다루는데, 실제로 사고가 나는 건 <b>"결과를 모르는" 세 번째 경우</b>입니다.
 * PG에 승인 요청을 보낸 뒤 타임아웃이 나면, 승인이 됐는지 안 됐는지 알 수 없습니다.
 * 이걸 실패로 처리하면 고객은 돈이 빠져나갔는데 주문은 없는 상태가 되고,
 * 성공으로 처리하면 받지도 않은 돈으로 상품을 보냅니다.
 *
 * <p>답은 "모른다"를 그대로 기록하고 나중에 PG에 다시 물어보는 것입니다(대사, reconciliation).
 */
public enum PaymentStatus {

    /** 승인 요청 직전. 이 레코드가 있으면 최소한 "시도했다"는 사실은 남습니다. */
    REQUESTED("승인 요청"),

    /** PG 승인 완료 */
    APPROVED("승인 완료"),

    /** PG가 명시적으로 거절 (한도 초과, 분실 카드 등) */
    FAILED("승인 실패"),

    /**
     * PG 호출은 했는데 결과를 모름 — 타임아웃, 네트워크 단절.
     * 대사 작업이 PG에 재조회해서 APPROVED 또는 FAILED로 확정해야 합니다.
     */
    UNKNOWN("확인 필요"),

    /** 승인 후 전액 취소 */
    CANCELLED("결제 취소"),

    /** 승인 후 부분 취소 */
    PARTIAL_CANCELLED("부분 취소"),

    /** 가상계좌 발급 완료, 입금 대기 중 */
    WAITING_FOR_DEPOSIT("입금 대기");

    private final String label;

    PaymentStatus(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    /** 돈이 실제로 우리 쪽에 들어온 상태인가 */
    public boolean isSettled() {
        return this == APPROVED || this == PARTIAL_CANCELLED;
    }

    /** 사람이 확인해야 하는 상태인가 */
    public boolean needsReconciliation() {
        return this == UNKNOWN;
    }
}

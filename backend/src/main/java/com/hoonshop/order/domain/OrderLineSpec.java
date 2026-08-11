package com.hoonshop.order.domain;

import com.hoonshop.common.domain.Money;

/**
 * 주문 항목 생성 명세.
 *
 * <p>{@link OrderLine} 생성자를 패키지 밖에 열지 않기 위한 입력 타입입니다.
 * 애플리케이션 계층은 "이런 항목을 담아 달라"고 명세만 넘기고, 실제 항목 조립과
 * 총액 계산은 {@link Order} 안에서 일어납니다 — 그래야 애그리거트를 거치지 않고
 * 항목을 만들어 끼워 넣는 경로가 생기지 않습니다.
 *
 * <p>가격 두 개를 모두 받는 이유는 주문 시점의 정가와 실제 판매가를 함께 박제하기 위해서입니다.
 * 값은 반드시 서버가 상품 저장소에서 읽은 것이어야 합니다.
 */
public record OrderLineSpec(
        String productCode,
        String productName,
        String colorId,
        String colorLabel,
        String size,
        int quantity,
        Money listPrice,
        Money unitPrice
) {
}

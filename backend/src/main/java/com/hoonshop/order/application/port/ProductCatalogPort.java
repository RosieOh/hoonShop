package com.hoonshop.order.application.port;

import com.hoonshop.common.domain.Money;

/**
 * 주문 → 카탈로그 방향의 부패 방지 계층(ACL).
 *
 * <p>주문 컨텍스트가 catalog의 {@code Product}를 직접 들고 다니면, 카탈로그 모델이 바뀔 때마다
 * 주문 코드가 따라 흔들립니다. 주문이 상품에 대해 알아야 하는 것은 사실 네 가지뿐입니다:
 * <b>이름, 정가, 판매가, 그리고 이 옵션 조합이 유효한가.</b> 딱 그만큼만 통과시킵니다.
 *
 * <p>구현은 {@code order.infrastructure}에 두어, 두 컨텍스트를 잇는 지점이 한 파일로 모입니다.
 */
public interface ProductCatalogPort {

    /**
     * 상품 스냅샷을 가져오면서 옵션 유효성까지 검증합니다.
     *
     * @throws com.hoonshop.common.domain.DomainException.NotFound 상품이 없을 때
     * @throws com.hoonshop.common.domain.DomainException.Conflict 없는 컬러/사이즈일 때
     */
    ProductSnapshot fetchForOrder(String productCode, String colorId, String size);

    record ProductSnapshot(String code, String name, String colorLabel, Money listPrice,
                           Money sellingPrice) {
    }
}

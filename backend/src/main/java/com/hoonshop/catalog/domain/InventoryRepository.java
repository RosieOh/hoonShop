package com.hoonshop.catalog.domain;

import java.util.List;
import java.util.Optional;

public interface InventoryRepository {

    Optional<Inventory> findByProductCode(ProductCode code);

    /**
     * 비관적 쓰기 락으로 조회합니다. 재고를 <b>차감</b>할 때만 씁니다.
     *
     * <p>조회용으로 쓰면 카탈로그 트래픽이 전부 직렬화됩니다.
     */
    Optional<Inventory> findForUpdate(ProductCode code);

    List<Inventory> findAllByProductCodes(List<ProductCode> codes);

    List<Inventory> findLowStock(int threshold);

    Inventory save(Inventory inventory);
}

package com.hoonshop.order.infrastructure;

import com.hoonshop.catalog.domain.Product;
import com.hoonshop.catalog.domain.ProductCode;
import com.hoonshop.catalog.domain.ProductRepository;
import com.hoonshop.common.domain.DomainException;
import com.hoonshop.order.application.port.ProductCatalogPort;
import org.springframework.stereotype.Component;

/**
 * {@link ProductCatalogPort} 구현 — 두 컨텍스트가 만나는 유일한 지점.
 *
 * <p>카탈로그 모델이 바뀌면 이 파일만 고치면 됩니다.
 */
@Component
public class CatalogAdapter implements ProductCatalogPort {

    private final ProductRepository products;

    public CatalogAdapter(ProductRepository products) {
        this.products = products;
    }

    @Override
    public ProductSnapshot fetchForOrder(String productCode, String colorId, String size) {
        Product product = products.findByCode(ProductCode.of(productCode))
                .orElseThrow(() -> new DomainException.NotFound("PRODUCT_NOT_FOUND",
                        "상품을 찾을 수 없습니다: " + productCode));

        // 옵션 검증은 상품 애그리거트가 합니다. 주문이 색 목록을 뒤지면 규칙이 두 곳에 생깁니다.
        product.validateOption(colorId, size);

        String colorLabel = product.colorOptions().stream()
                .filter(c -> c.id().equals(colorId))
                .findFirst()
                .map(c -> c.label())
                .orElse(colorId);

        return new ProductSnapshot(product.code().value(), product.name(), colorLabel,
                product.listPrice(), product.sellingPrice());
    }
}

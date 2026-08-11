package com.hoonshop.catalog.application;

import com.hoonshop.catalog.domain.*;
import com.hoonshop.common.domain.DomainException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * 상품 조회 유스케이스.
 *
 * <p>애플리케이션 서비스는 얇게 유지합니다 — 여기서 하는 일은 저장소를 부르고, 두
 * 애그리거트를 조합하고, 트랜잭션 경계를 긋는 것뿐입니다. 판매가 계산이나 옵션 검증 같은
 * 규칙은 전부 도메인 안에 있습니다.
 */
@Service
@Transactional(readOnly = true)
public class ProductQueryService {

    private final ProductRepository products;
    private final InventoryRepository inventories;

    public ProductQueryService(ProductRepository products, InventoryRepository inventories) {
        this.products = products;
        this.inventories = inventories;
    }

    public PagedProducts search(ProductSearchCommand command) {
        ProductSearchResult result = products.search(command);
        List<ProductView> views = combine(result.items());
        return new PagedProducts(views, result.page(), result.size(), result.total(),
                result.hasNext());
    }

    public ProductDetail detail(ProductCode code) {
        Product product = products.findByCode(code)
                .orElseThrow(() -> new DomainException.NotFound("PRODUCT_NOT_FOUND",
                        "상품을 찾을 수 없습니다: " + code.value()));

        Inventory inventory = inventories.findByProductCode(code).orElse(null);
        List<Product> related = products.findRelated(product.category(), code, 4);

        return new ProductDetail(ProductView.of(product, inventory), combine(related));
    }

    /**
     * 상품 목록에 재고를 붙입니다.
     *
     * <p>상품마다 재고를 한 번씩 조회하면 N+1이 납니다. 코드를 모아 한 번에 조회한 뒤
     * 메모리에서 매칭합니다.
     */
    private List<ProductView> combine(List<Product> items) {
        if (items.isEmpty()) {
            return List.of();
        }
        List<ProductCode> codes = items.stream().map(Product::code).toList();
        Map<String, Inventory> stockByCode = inventories.findAllByProductCodes(codes).stream()
                .collect(java.util.stream.Collectors.toMap(
                        i -> i.productCode().value(), Function.identity()));

        return items.stream()
                .map(p -> ProductView.of(p, stockByCode.get(p.code().value())))
                .toList();
    }

    public record PagedProducts(List<ProductView> items, int page, int size, long total,
                                boolean hasNext) {
    }

    public record ProductDetail(ProductView product, List<ProductView> related) {
    }
}

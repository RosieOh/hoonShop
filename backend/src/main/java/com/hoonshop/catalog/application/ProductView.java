package com.hoonshop.catalog.application;

import com.hoonshop.catalog.domain.ColorOption;
import com.hoonshop.catalog.domain.Inventory;
import com.hoonshop.catalog.domain.Product;

import java.time.Instant;
import java.util.List;

/**
 * 상품 + 재고를 합친 조회 모델.
 *
 * <p>두 애그리거트를 화면 하나에 같이 보여줘야 하는데, 그렇다고 애그리거트를 합치면
 * 재고 락이 카탈로그 조회를 막습니다. 이렇게 <b>애플리케이션 계층에서 조합</b>하는 것이
 * 애그리거트를 잘게 유지하면서 화면 요구를 만족시키는 표준적인 방법입니다.
 */
public record ProductView(
        String id,
        String name,
        String category,
        String colorway,
        List<String> palette,
        long price,
        Long salePrice,
        int discountRate,
        double rating,
        int reviewCount,
        List<String> badges,
        String description,
        List<String> materials,
        List<String> sizes,
        List<ColorOptionView> colorOptions,
        int stock,
        Instant createdAt,
        int soldCount
) {

    public record ColorOptionView(String id, String label, String hex) {
        static ColorOptionView from(ColorOption option) {
            return new ColorOptionView(option.id(), option.label(), option.hex());
        }
    }

    public static ProductView of(Product product, Inventory inventory) {
        boolean discounted = product.discountRate() > 0;
        return new ProductView(
                product.code().value(),
                product.name(),
                product.category().code(),
                product.colorway(),
                product.palette(),
                product.listPrice().value(),
                discounted ? product.sellingPrice().value() : null,
                product.discountRate(),
                product.rating(),
                product.reviewCount(),
                product.badges().stream().map(b -> b.code()).toList(),
                product.description(),
                product.materials(),
                product.sizes(),
                product.colorOptions().stream().map(ColorOptionView::from).toList(),
                inventory == null ? 0 : inventory.quantity(),
                product.createdAt(),
                product.soldCount());
    }
}

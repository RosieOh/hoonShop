package com.hoonshop.catalog.presentation;

import com.hoonshop.catalog.application.ProductQueryService;
import com.hoonshop.catalog.application.ProductView;
import com.hoonshop.catalog.domain.Badge;
import com.hoonshop.catalog.domain.Category;
import com.hoonshop.catalog.domain.ProductCode;
import com.hoonshop.catalog.domain.ProductSearchCommand;
import com.hoonshop.common.domain.Money;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

/**
 * 상품 조회 API.
 *
 * <p>프레젠테이션 계층의 책임은 HTTP를 도메인 언어로 번역하는 것뿐입니다.
 * "category=all"이 "필터 없음"을 뜻한다는 것 같은 API 관례를 여기서 흡수하고,
 * 도메인은 {@code null = 전체}만 알면 됩니다.
 */
@Tag(name = "Product", description = "상품 조회")
@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductQueryService productQueryService;

    public ProductController(ProductQueryService productQueryService) {
        this.productQueryService = productQueryService;
    }

    @Operation(summary = "상품 목록 조회 (필터·정렬·페이지네이션)")
    @GetMapping
    public ProductQueryService.PagedProducts list(
            @RequestParam(required = false, defaultValue = "all") String category,
            @RequestParam(required = false, defaultValue = "recommend") String sort,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Long minPrice,
            @RequestParam(required = false) Long maxPrice,
            @RequestParam(required = false) String colors,
            @RequestParam(required = false) String badge,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "12") int size) {

        ProductSearchCommand command = new ProductSearchCommand(
                parseCategory(category),
                q,
                minPrice == null ? null : Money.won(minPrice),
                maxPrice == null ? null : Money.won(maxPrice),
                parseColors(colors),
                parseBadge(badge),
                ProductSearchCommand.SortBy.fromCode(sort),
                page,
                size);

        return productQueryService.search(command);
    }

    @Operation(summary = "상품 상세 조회 (연관 상품 포함)")
    @GetMapping("/{code}")
    public ProductDetailResponse detail(@PathVariable String code) {
        ProductQueryService.ProductDetail detail =
                productQueryService.detail(ProductCode.of(code));
        return ProductDetailResponse.of(detail);
    }

    private Category parseCategory(String category) {
        if (category == null || category.isBlank() || "all".equalsIgnoreCase(category)) {
            return null;
        }
        return Category.fromCode(category);
    }

    private List<String> parseColors(String colors) {
        if (colors == null || colors.isBlank()) {
            return List.of();
        }
        return Arrays.stream(colors.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
    }

    private Badge parseBadge(String badge) {
        if (badge == null || badge.isBlank()) {
            return null;
        }
        return Arrays.stream(Badge.values())
                .filter(b -> b.code().equalsIgnoreCase(badge))
                .findFirst()
                .orElse(null);
    }

    /**
     * 프론트가 기대하는 형태({@code {...product, related}})에 맞춥니다.
     * 도메인 모델을 이 형태로 만들지 않고 여기서 펼치는 이유는, 화면 요구가 바뀌어도
     * 도메인이 흔들리지 않게 하기 위해서입니다.
     */
    public record ProductDetailResponse(
            String id, String name, String category, String colorway, List<String> palette,
            long price, Long salePrice, int discountRate, double rating, int reviewCount,
            List<String> badges, String description, List<String> materials, List<String> sizes,
            List<ProductView.ColorOptionView> colorOptions, int stock, String createdAt,
            int soldCount, List<ProductView> related) {

        static ProductDetailResponse of(ProductQueryService.ProductDetail detail) {
            ProductView p = detail.product();
            return new ProductDetailResponse(p.id(), p.name(), p.category(), p.colorway(),
                    p.palette(), p.price(), p.salePrice(), p.discountRate(), p.rating(),
                    p.reviewCount(), p.badges(), p.description(), p.materials(), p.sizes(),
                    p.colorOptions(), p.stock(), p.createdAt().toString(), p.soldCount(),
                    detail.related());
        }
    }
}

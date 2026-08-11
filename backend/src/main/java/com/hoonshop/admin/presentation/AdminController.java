package com.hoonshop.admin.presentation;

import com.hoonshop.admin.application.AdminStatsService;
import com.hoonshop.catalog.application.InventoryService;
import com.hoonshop.catalog.application.ProductQueryService;
import com.hoonshop.catalog.domain.ProductCode;
import com.hoonshop.catalog.domain.ProductSearchCommand;
import com.hoonshop.order.application.OrderQueryService;
import com.hoonshop.order.application.OrderView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 관리자 API.
 *
 * <p>경로가 {@code /api/admin/**}이면 {@code SecurityConfig}에서 ROLE_ADMIN을 요구합니다 —
 * 컨트롤러마다 권한 검사를 반복하지 않고 한 곳에서 막습니다.
 */
@Tag(name = "Admin", description = "관리자")
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminStatsService statsService;
    private final OrderQueryService orderQueryService;
    private final ProductQueryService productQueryService;
    private final InventoryService inventoryService;

    public AdminController(AdminStatsService statsService, OrderQueryService orderQueryService,
                           ProductQueryService productQueryService,
                           InventoryService inventoryService) {
        this.statsService = statsService;
        this.orderQueryService = orderQueryService;
        this.productQueryService = productQueryService;
        this.inventoryService = inventoryService;
    }

    @Operation(summary = "대시보드 집계")
    @GetMapping("/stats")
    public AdminStatsService.Stats stats() {
        return statsService.stats();
    }

    @Operation(summary = "주문 목록 (상태·키워드 필터)")
    @GetMapping("/orders")
    public OrderListResponse orders(@RequestParam(required = false) String status,
                                    @RequestParam(required = false) String q) {
        List<OrderView> items = orderQueryService.searchForAdmin(status, q);
        return new OrderListResponse(items, items.size());
    }

    @Operation(summary = "주문 상태 변경")
    @PatchMapping("/orders/{orderNumber}")
    public OrderView changeOrderStatus(@PathVariable String orderNumber,
                                       @RequestBody StatusChangeRequest request) {
        return orderQueryService.changeStatus(orderNumber, request.status());
    }

    @Operation(summary = "상품·재고 목록 (재고 적은 순)")
    @GetMapping("/products")
    public ProductListResponse products(@RequestParam(required = false) String q) {
        // 관리자 화면은 재고가 적은 것부터 봐야 하므로 페이지 크기를 크게 잡고 재고순으로 정렬합니다.
        var command = new ProductSearchCommand(null, q, null, null, List.of(), null,
                ProductSearchCommand.SortBy.RECOMMEND, 1, 100);

        List<com.hoonshop.catalog.application.ProductView> items =
                productQueryService.search(command).items().stream()
                        .sorted(java.util.Comparator.comparingInt(
                                com.hoonshop.catalog.application.ProductView::stock))
                        .toList();

        return new ProductListResponse(items, items.size());
    }

    @Operation(summary = "재고 수정")
    @PatchMapping("/products/{code}")
    public StockResponse adjustStock(@PathVariable String code,
                                     @RequestBody StockAdjustRequest request) {
        int quantity = inventoryService.adjust(ProductCode.of(code), request.stock());
        return new StockResponse(code, quantity);
    }

    public record OrderListResponse(List<OrderView> items, int total) {
    }

    public record ProductListResponse(
            List<com.hoonshop.catalog.application.ProductView> items, int total) {
    }

    public record StatusChangeRequest(@NotBlank String status) {
    }

    public record StockAdjustRequest(int stock) {
    }

    public record StockResponse(String id, int stock) {
    }
}

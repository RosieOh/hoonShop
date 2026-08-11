package com.hoonshop.catalog.application;

import com.hoonshop.catalog.domain.*;
import com.hoonshop.common.domain.DomainException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * 재고 유스케이스.
 *
 * <p>차감({@link #deductAll})은 결제 승인 흐름에서 <b>같은 트랜잭션 안에</b> 호출됩니다.
 * 별도 트랜잭션으로 떼면 결제는 승인됐는데 재고 차감이 실패하는 상태가 생깁니다.
 */
@Service
@Transactional(readOnly = true)
public class InventoryService {

    private static final int LOW_STOCK_THRESHOLD = 5;

    private final InventoryRepository inventories;
    private final ProductRepository products;

    public InventoryService(InventoryRepository inventories, ProductRepository products) {
        this.inventories = inventories;
        this.products = products;
    }

    /** 결제 전 가용성 확인. 락을 걸지 않으므로 "확인 시점의 스냅샷"일 뿐입니다. */
    public List<StockIssue> check(List<StockRequest> requests) {
        Map<String, Inventory> byCode = inventories.findAllByProductCodes(
                        requests.stream().map(r -> ProductCode.of(r.productCode())).toList())
                .stream()
                .collect(java.util.stream.Collectors.toMap(i -> i.productCode().value(),
                        Function.identity()));

        Map<String, Product> productByCode = products.findAllByCodes(
                        requests.stream().map(r -> ProductCode.of(r.productCode())).toList())
                .stream()
                .collect(java.util.stream.Collectors.toMap(p -> p.code().value(),
                        Function.identity()));

        return requests.stream()
                .map(request -> {
                    Inventory inventory = byCode.get(request.productCode());
                    Product product = productByCode.get(request.productCode());
                    String name = product == null ? request.productCode() : product.name();

                    if (inventory == null) {
                        return new StockIssue(request.productCode(), name, "NOT_FOUND", 0);
                    }
                    if (inventory.isSoldOut()) {
                        return new StockIssue(request.productCode(), name, "SOLD_OUT", 0);
                    }
                    if (!inventory.canFulfill(request.quantity())) {
                        return new StockIssue(request.productCode(), name, "INSUFFICIENT",
                                inventory.quantity());
                    }
                    return null;
                })
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    /**
     * 실제 차감. 락을 걸고 하나씩 뺍니다.
     *
     * <p>여러 상품을 뺄 때 코드 순으로 정렬하는 이유는 데드락 방지입니다.
     * A→B 순으로 락을 잡는 트랜잭션과 B→A 순으로 잡는 트랜잭션이 만나면 서로를 기다립니다.
     * 항상 같은 순서로 잡으면 그 상황이 생기지 않습니다.
     */
    @Transactional
    public void deductAll(List<StockRequest> requests) {
        requests.stream()
                .sorted(java.util.Comparator.comparing(StockRequest::productCode))
                .forEach(request -> {
                    ProductCode code = ProductCode.of(request.productCode());
                    Inventory inventory = inventories.findForUpdate(code)
                            .orElseThrow(() -> new DomainException.NotFound("PRODUCT_NOT_FOUND",
                                    "재고 정보를 찾을 수 없습니다: " + code.value()));
                    inventory.deduct(request.quantity());
                    inventories.save(inventory);
                });
    }

    @Transactional
    public void restoreAll(List<StockRequest> requests) {
        requests.forEach(request -> {
            ProductCode code = ProductCode.of(request.productCode());
            inventories.findForUpdate(code).ifPresent(inventory -> {
                inventory.restore(request.quantity());
                inventories.save(inventory);
            });
        });
    }

    @Transactional
    public int adjust(ProductCode code, int quantity) {
        Inventory inventory = inventories.findForUpdate(code)
                .orElseThrow(() -> new DomainException.NotFound("PRODUCT_NOT_FOUND",
                        "재고 정보를 찾을 수 없습니다: " + code.value()));
        inventory.adjustTo(quantity);
        inventories.save(inventory);
        return inventory.quantity();
    }

    public List<LowStockItem> lowStock() {
        List<Inventory> low = inventories.findLowStock(LOW_STOCK_THRESHOLD);
        Map<String, Product> byCode = products.findAllByCodes(
                        low.stream().map(Inventory::productCode).toList())
                .stream()
                .collect(java.util.stream.Collectors.toMap(p -> p.code().value(),
                        Function.identity()));

        return low.stream()
                .map(i -> new LowStockItem(i.productCode().value(),
                        byCode.containsKey(i.productCode().value())
                                ? byCode.get(i.productCode().value()).name()
                                : i.productCode().value(),
                        i.quantity()))
                .toList();
    }

    public record StockRequest(String productCode, int quantity) {
    }

    public record StockIssue(String productId, String name, String reason, int available) {
    }

    public record LowStockItem(String id, String name, int stock) {
    }
}

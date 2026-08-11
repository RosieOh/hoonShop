package com.hoonshop.catalog.infrastructure;

import com.hoonshop.catalog.application.InventoryService;
import com.hoonshop.order.domain.OrderCancelled;
import com.hoonshop.order.domain.OrderPaid;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 주문 이벤트 → 재고 반영.
 *
 * <p>order 컨텍스트가 catalog를 직접 호출하지 않는 덕에, 나중에 "결제 완료 시 포인트 적립",
 * "품절 시 알림 발송" 같은 후속 처리를 추가할 때 주문 코드를 건드릴 필요가 없습니다.
 * 구독자만 하나 늘리면 됩니다.
 */
@Component
public class OrderEventListener {

    private final InventoryService inventoryService;

    public OrderEventListener(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    /** 결제 완료 시점에만 재고가 빠집니다. 주문서 작성 단계에서는 선점하지 않습니다. */
    @EventListener
    public void on(OrderPaid event) {
        inventoryService.deductAll(toRequests(event.stockChanges()));
    }

    /**
     * 취소 시 복원.
     *
     * <p>{@code wasPaid}가 false면 애초에 차감된 적이 없으므로 되돌리지 않습니다 —
     * 무조건 복원하면 결제 전 취소마다 없던 재고가 생깁니다.
     */
    @EventListener
    public void on(OrderCancelled event) {
        if (!event.wasPaid()) {
            return;
        }
        inventoryService.restoreAll(toRequests(event.stockChanges()));
    }

    private List<InventoryService.StockRequest> toRequests(List<OrderPaid.StockChange> changes) {
        return changes.stream()
                .map(c -> new InventoryService.StockRequest(c.productCode(), c.quantity()))
                .toList();
    }
}

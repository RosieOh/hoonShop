package com.hoonshop.order.presentation;

import com.hoonshop.catalog.application.InventoryService;
import com.hoonshop.common.domain.DomainException;
import com.hoonshop.order.application.CancelOrderService;
import com.hoonshop.order.application.OrderQueryService;
import com.hoonshop.order.application.OrderView;
import com.hoonshop.order.application.PlaceOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Order", description = "주문")
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final PlaceOrderService placeOrderService;
    private final OrderQueryService orderQueryService;
    private final CancelOrderService cancelOrderService;
    private final InventoryService inventoryService;

    public OrderController(PlaceOrderService placeOrderService,
                           OrderQueryService orderQueryService,
                           CancelOrderService cancelOrderService,
                           InventoryService inventoryService) {
        this.placeOrderService = placeOrderService;
        this.orderQueryService = orderQueryService;
        this.cancelOrderService = cancelOrderService;
        this.inventoryService = inventoryService;
    }

    @Operation(summary = "결제 전 재고 확인 — 품절·수량 부족 항목을 미리 알려줍니다")
    @PostMapping("/validate")
    public StockCheckResponse validate(@RequestBody @Valid StockCheckRequest request) {
        List<InventoryService.StockIssue> issues = inventoryService.check(
                request.items().stream()
                        .map(i -> new InventoryService.StockRequest(i.productId(), i.quantity()))
                        .toList());
        return new StockCheckResponse(issues.isEmpty(), issues);
    }

    @Operation(summary = "주문 생성 — 금액은 서버가 다시 계산합니다")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderView place(@RequestBody @Valid PlaceOrderRequest request,
                           Authentication authentication) {
        String email = requireEmail(authentication);

        var command = new PlaceOrderService.PlaceOrderCommand(
                email,
                request.customerName(),
                request.items().stream()
                        .map(i -> new PlaceOrderService.PlaceOrderCommand.Item(
                                i.productId(), i.color(), i.size(), i.quantity()))
                        .toList(),
                request.shippingAddress().recipient(),
                request.shippingAddress().phone(),
                request.shippingAddress().zipcode(),
                request.shippingAddress().address1(),
                request.shippingAddress().address2(),
                request.deliveryMemo(),
                request.couponIds(),
                request.amount());

        return OrderView.from(placeOrderService.place(command));
    }

    @Operation(summary = "내 주문 목록")
    @GetMapping
    public OrderListResponse myOrders(Authentication authentication) {
        return new OrderListResponse(orderQueryService.myOrders(requireEmail(authentication)));
    }

    @Operation(summary = "내 주문 상세")
    @GetMapping("/{orderNumber}")
    public OrderView myOrder(@PathVariable String orderNumber, Authentication authentication) {
        return orderQueryService.myOrder(orderNumber, requireEmail(authentication));
    }

    @Operation(summary = "주문 취소",
            description = "결제된 주문이면 환불 후 취소합니다. 발송 이후에는 취소할 수 없습니다.")
    @PostMapping("/{orderNumber}/cancel")
    public OrderView cancel(@PathVariable String orderNumber,
                            @RequestBody(required = false) CancelRequest request,
                            Authentication authentication) {
        String reason = (request == null || request.reason() == null || request.reason().isBlank())
                ? "고객 요청"
                : request.reason();
        return cancelOrderService.cancel(orderNumber, reason, requireEmail(authentication), false);
    }

    private String requireEmail(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new DomainException.Unauthorized("UNAUTHENTICATED", "로그인이 필요합니다.");
        }
        return authentication.getName();
    }

    /* ------------------------------------------------------------ DTO --- */

    public record StockCheckRequest(@NotEmpty List<Item> items) {
        public record Item(@NotBlank String productId, int quantity) {
        }
    }

    public record StockCheckResponse(boolean ok, List<InventoryService.StockIssue> issues) {
    }

    public record PlaceOrderRequest(
            @NotBlank String customerName,
            @NotEmpty List<Item> items,
            @Valid ShippingAddressRequest shippingAddress,
            String deliveryMemo,
            List<String> couponIds,
            /** 프론트가 표시한 금액. 저장에는 쓰지 않고 서버 계산과 대조만 합니다. */
            Long amount) {

        public record Item(@NotBlank String productId, @NotBlank String color, String size,
                           int quantity) {
        }
    }

    public record ShippingAddressRequest(@NotBlank String recipient, @NotBlank String phone,
                                         @NotBlank String zipcode, @NotBlank String address1,
                                         String address2) {
    }

    public record OrderListResponse(List<OrderView> items) {
    }

    public record CancelRequest(String reason) {
    }
}

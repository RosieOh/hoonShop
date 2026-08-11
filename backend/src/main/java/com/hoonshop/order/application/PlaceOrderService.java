package com.hoonshop.order.application;

import com.hoonshop.common.domain.Money;
import com.hoonshop.order.application.port.CouponPort;
import com.hoonshop.order.application.port.ProductCatalogPort;
import com.hoonshop.order.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 주문 생성 유스케이스.
 *
 * <p>여기서 지키는 원칙 하나: <b>클라이언트가 보낸 금액은 어디에도 반영되지 않습니다.</b>
 * 요청에서 가져오는 건 "무엇을 몇 개, 어떤 옵션으로, 어디로, 어떤 쿠폰으로"까지이고,
 * 단가·할인·배송비·최종 금액은 전부 서버가 다시 만듭니다.
 * 요청의 {@code amount}는 마지막에 대조용으로만 씁니다.
 */
@Service
public class PlaceOrderService {

    private final OrderRepository orders;
    private final ProductCatalogPort catalog;
    private final CouponPort coupons;

    public PlaceOrderService(OrderRepository orders, ProductCatalogPort catalog,
                             CouponPort coupons) {
        this.orders = orders;
        this.catalog = catalog;
        this.coupons = coupons;
    }

    @Transactional
    public Order place(PlaceOrderCommand command) {
        // 1) 상품 정보와 가격을 서버가 직접 읽습니다. 옵션 유효성도 이 단계에서 걸러집니다.
        List<OrderLineSpec> specs = command.items().stream()
                .map(item -> {
                    var snapshot = catalog.fetchForOrder(item.productCode(), item.colorId(),
                            item.size());
                    return new OrderLineSpec(snapshot.code(), snapshot.name(), item.colorId(),
                            snapshot.colorLabel(), item.size(), item.quantity(),
                            snapshot.listPrice(), snapshot.sellingPrice());
                })
                .toList();

        // 2) 쿠폰 할인도 서버 계산. 배송비 규칙은 도메인 정책을 그대로 씁니다.
        Money payableItems = specs.stream()
                .map(s -> s.unitPrice().times(s.quantity()))
                .reduce(Money.ZERO, Money::plus);
        Money shippingFee = ShippingPolicy.feeFor(payableItems);

        CouponPort.Discount discount =
                coupons.calculate(command.couponCodes(), payableItems, shippingFee);

        // 3) 애그리거트가 총액을 확정합니다.
        Order order = Order.place(
                OrderNumber.issue(orders.nextSequence()),
                command.customerEmail(),
                command.customerName(),
                specs,
                ShippingAddress.of(command.recipient(), command.phone(), command.zipcode(),
                        command.address1(), command.address2()),
                command.deliveryMemo(),
                command.couponCodes(),
                discount.itemDiscount(),
                discount.shippingDiscount());

        // 4) 프론트가 보여준 금액과 다르면 결제로 넘기지 않습니다.
        if (command.expectedAmount() != null) {
            order.assertPayableMatches(Money.won(command.expectedAmount()));
        }

        return orders.save(order);
    }

    public record PlaceOrderCommand(
            String customerEmail,
            String customerName,
            List<Item> items,
            String recipient,
            String phone,
            String zipcode,
            String address1,
            String address2,
            String deliveryMemo,
            List<String> couponCodes,
            Long expectedAmount
    ) {
        public PlaceOrderCommand {
            couponCodes = couponCodes == null ? List.of() : List.copyOf(couponCodes);
        }

        public record Item(String productCode, String colorId, String size, int quantity) {
        }
    }
}

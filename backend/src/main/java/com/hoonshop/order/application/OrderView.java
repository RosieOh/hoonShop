package com.hoonshop.order.application;

import com.hoonshop.order.domain.Order;
import com.hoonshop.order.domain.OrderLine;

import java.time.Instant;
import java.util.List;

/** 주문 조회 모델. 도메인 객체를 그대로 직렬화하지 않고 화면이 필요한 모양으로 옮깁니다. */
public record OrderView(
        String id,
        Customer customer,
        List<Item> items,
        ShippingAddressView shippingAddress,
        String deliveryMemo,
        List<String> couponIds,
        long amount,
        Amounts amounts,
        String status,
        String statusLabel,
        Instant createdAt,
        Instant paidAt
) {

    public record Customer(String name, String email) {
    }

    public record Item(String productId, String name, Options options, int quantity, long price) {
    }

    public record Options(String color, String colorLabel, String size) {
    }

    public record ShippingAddressView(String recipient, String phone, String zipcode,
                                      String address1, String address2) {
    }

    public record Amounts(long itemsListTotal, long itemDiscount, long couponDiscount,
                          long shippingFee, long shippingDiscount, long payable) {
    }

    public static OrderView from(Order order) {
        return new OrderView(
                order.orderNumber().value(),
                new Customer(order.customerName(), order.customerEmail()),
                order.lines().stream().map(OrderView::toItem).toList(),
                new ShippingAddressView(
                        order.shippingAddress().recipient(),
                        order.shippingAddress().phone(),
                        order.shippingAddress().zipcode(),
                        order.shippingAddress().address1(),
                        order.shippingAddress().address2()),
                order.deliveryMemo(),
                order.couponCodes(),
                order.amounts().payable().value(),
                new Amounts(
                        order.amounts().itemsListTotal().value(),
                        order.amounts().itemDiscount().value(),
                        order.amounts().couponDiscount().value(),
                        order.amounts().shippingFee().value(),
                        order.amounts().shippingDiscount().value(),
                        order.amounts().payable().value()),
                order.status().name(),
                order.status().label(),
                order.createdAt(),
                order.paidAt());
    }

    private static Item toItem(OrderLine line) {
        return new Item(line.productCode(), line.productName(),
                new Options(line.colorId(), line.colorLabel(), line.size()),
                line.quantity(), line.unitPrice().value());
    }
}

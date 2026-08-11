package com.hoonshop.order.domain;

import com.hoonshop.common.domain.AggregateRoot;
import com.hoonshop.common.domain.DomainException;
import com.hoonshop.common.domain.Money;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * 주문 애그리거트 루트.
 *
 * <p>이 클래스가 지키는 불변식:
 * <ul>
 *   <li>항목이 하나도 없는 주문은 존재할 수 없다</li>
 *   <li>결제 금액은 항목 합계 − 할인 + 배송비로만 결정된다 (외부에서 주입 불가)</li>
 *   <li>상태 전이는 {@link OrderStatus}가 허용한 경로로만 가능하다</li>
 * </ul>
 *
 * <p><b>금액을 인자로 받지 않는 것이 핵심입니다.</b> 클라이언트가 계산한 결제 금액을 받아
 * 저장하면 요청을 조작해 1원 결제가 가능해집니다. {@code place()}는 항목과 쿠폰 할인만 받고
 * 총액은 스스로 계산합니다.
 */
@Entity
@Table(name = "orders")
public class Order extends AggregateRoot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Embedded
    private OrderNumber orderNumber;

    @Column(name = "customer_email", nullable = false, length = 120)
    private String customerEmail;

    @Column(name = "customer_name", nullable = false, length = 40)
    private String customerName;

    /**
     * 항목은 주문과 생명주기를 공유합니다 — 주문이 지워지면 항목도 지워지고,
     * 항목만 따로 조회할 이유가 없습니다. 그래서 cascade + orphanRemoval입니다.
     */
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private List<OrderLine> lines = new ArrayList<>();

    @Embedded
    private ShippingAddress shippingAddress;

    @Column(name = "delivery_memo", length = 100)
    private String deliveryMemo;

    @Embedded
    private OrderAmounts amounts;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "order_coupon", joinColumns = @JoinColumn(name = "order_id"))
    @Column(name = "coupon_code", nullable = false, length = 40)
    private List<String> couponCodes = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status;

    @Column(nullable = false)
    private Instant createdAt;

    private Instant paidAt;

    @Version
    private Long version;

    protected Order() {
    }

    /**
     * 주문 생성.
     *
     * @param specs          서버가 상품 저장소에서 읽은 가격으로 만든 항목 명세
     * @param couponDiscount 서버가 쿠폰 정책으로 다시 계산한 할인액
     */
    public static Order place(OrderNumber orderNumber, String customerEmail, String customerName,
                              List<OrderLineSpec> specs, ShippingAddress shippingAddress,
                              String deliveryMemo, List<String> couponCodes,
                              Money couponDiscount, Money shippingCouponDiscount) {

        if (specs == null || specs.isEmpty()) {
            throw new DomainException.Conflict("EMPTY_ORDER", "주문할 상품이 없습니다.");
        }

        Order order = new Order();
        order.orderNumber = orderNumber;
        order.customerEmail = customerEmail;
        order.customerName = customerName;
        order.lines = specs.stream()
                .map(s -> OrderLine.of(s.productCode(), s.productName(), s.colorId(),
                        s.colorLabel(), s.size(), s.quantity(), s.listPrice(), s.unitPrice()))
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        order.shippingAddress = shippingAddress;
        order.deliveryMemo = deliveryMemo == null ? "" : deliveryMemo;
        order.couponCodes = new ArrayList<>(couponCodes);
        order.status = OrderStatus.PAYMENT_PENDING;
        order.createdAt = Instant.now();

        Money itemsListTotal = order.sum(OrderLine::lineListTotal);
        Money itemDiscount = order.sum(OrderLine::lineDiscount);
        Money shippingFee = ShippingPolicy.feeFor(itemsListTotal.minus(itemDiscount));

        order.amounts = OrderAmounts.of(itemsListTotal, itemDiscount, couponDiscount, shippingFee,
                shippingCouponDiscount.min(shippingFee));

        order.registerEvent(new OrderPlaced(orderNumber, customerEmail,
                order.amounts.payable(), Instant.now()));
        return order;
    }

    private Money sum(java.util.function.Function<OrderLine, Money> extractor) {
        return lines.stream().map(extractor).reduce(Money.ZERO, Money::plus);
    }

    /**
     * 클라이언트가 보낸 결제 예정 금액과 서버 계산이 일치하는지 확인합니다.
     *
     * <p>불일치를 조용히 서버 값으로 덮으면 프론트 버그를 영영 발견하지 못합니다.
     * 결제를 막고 드러냅니다.
     */
    public void assertPayableMatches(Money clientAmount) {
        if (!amounts.payable().equals(clientAmount)) {
            throw new DomainException.Conflict("AMOUNT_MISMATCH",
                    "결제 금액이 서버 계산과 다릅니다. 주문서를 새로고침해 주세요. (서버 %s / 요청 %s)"
                            .formatted(amounts.payable(), clientAmount));
        }
    }

    /** 결제 승인 완료. 여기서부터 재고가 빠집니다. */
    public void markPaid() {
        status.ensureCanTransitionTo(OrderStatus.PAID);
        this.status = OrderStatus.PAID;
        this.paidAt = Instant.now();
        registerEvent(new OrderPaid(orderNumber, stockRequests(), Instant.now()));
    }

    /** 관리자 상태 변경. */
    public void changeStatus(OrderStatus target) {
        status.ensureCanTransitionTo(target);
        this.status = target;
    }

    public void cancel(String reason) {
        if (status.isTerminal()) {
            throw new DomainException.Conflict("ALREADY_CLOSED",
                    "이미 종료된 주문은 취소할 수 없습니다.");
        }
        boolean wasPaid = status != OrderStatus.PAYMENT_PENDING;
        status.ensureCanTransitionTo(OrderStatus.CANCELLED);
        this.status = OrderStatus.CANCELLED;
        registerEvent(new OrderCancelled(orderNumber, stockRequests(), wasPaid, reason,
                Instant.now()));
    }

    /** 재고 차감/복원에 필요한 최소 정보만 이벤트로 넘깁니다. */
    private List<OrderPaid.StockChange> stockRequests() {
        return lines.stream()
                .map(l -> new OrderPaid.StockChange(l.productCode(), l.quantity()))
                .toList();
    }

    public boolean isOwnedBy(String email) {
        return customerEmail.equalsIgnoreCase(email);
    }

    public OrderNumber orderNumber() {
        return orderNumber;
    }

    public String customerEmail() {
        return customerEmail;
    }

    public String customerName() {
        return customerName;
    }

    public List<OrderLine> lines() {
        return List.copyOf(lines);
    }

    public ShippingAddress shippingAddress() {
        return shippingAddress;
    }

    public String deliveryMemo() {
        return deliveryMemo;
    }

    public OrderAmounts amounts() {
        return amounts;
    }

    public List<String> couponCodes() {
        return List.copyOf(couponCodes);
    }

    public OrderStatus status() {
        return status;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant paidAt() {
        return paidAt;
    }
}

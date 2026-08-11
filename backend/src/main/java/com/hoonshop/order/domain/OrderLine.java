package com.hoonshop.order.domain;

import com.hoonshop.common.domain.Money;
import jakarta.persistence.*;

/**
 * 주문 항목 — {@link Order} 애그리거트 내부 엔티티.
 *
 * <p>애그리거트 밖에서 직접 조회하거나 수정할 수 없습니다. 반드시 Order를 통해 다뤄야
 * "주문 총액 = 항목 합계"라는 불변식이 깨지지 않습니다.
 *
 * <p><b>가격을 복사해 둡니다.</b> 상품 코드만 들고 있다가 나중에 현재가를 조회하면,
 * 판매가가 바뀐 순간 과거 주문의 결제 금액이 소급해서 달라집니다. 영수증은 그때 그 값이어야 합니다.
 */
@Entity
@Table(name = "order_line")
public class OrderLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_code", nullable = false, length = 16)
    private String productCode;

    @Column(name = "product_name", nullable = false, length = 100)
    private String productName;

    @Column(name = "color_id", nullable = false, length = 32)
    private String colorId;

    @Column(name = "color_label", nullable = false, length = 32)
    private String colorLabel;

    @Column(name = "size_label", length = 40)
    private String size;

    @Column(nullable = false)
    private int quantity;

    /** 주문 시점의 정가 */
    @AttributeOverride(name = "amount", column = @Column(name = "list_price", nullable = false))
    private Money listPrice;

    /** 주문 시점의 실제 판매 단가 */
    @AttributeOverride(name = "amount", column = @Column(name = "unit_price", nullable = false))
    private Money unitPrice;

    protected OrderLine() {
    }

    private OrderLine(String productCode, String productName, String colorId, String colorLabel,
                      String size, int quantity, Money listPrice, Money unitPrice) {
        if (quantity < 1 || quantity > 10) {
            throw new IllegalArgumentException("수량은 1~10개 사이여야 합니다: " + quantity);
        }
        this.productCode = productCode;
        this.productName = productName;
        this.colorId = colorId;
        this.colorLabel = colorLabel;
        this.size = size;
        this.quantity = quantity;
        this.listPrice = listPrice;
        this.unitPrice = unitPrice;
    }

    static OrderLine of(String productCode, String productName, String colorId, String colorLabel,
                        String size, int quantity, Money listPrice, Money unitPrice) {
        return new OrderLine(productCode, productName, colorId, colorLabel, size, quantity,
                listPrice, unitPrice);
    }

    public Money lineTotal() {
        return unitPrice.times(quantity);
    }

    public Money lineListTotal() {
        return listPrice.times(quantity);
    }

    public Money lineDiscount() {
        return lineListTotal().minus(lineTotal());
    }

    public String productCode() {
        return productCode;
    }

    public String productName() {
        return productName;
    }

    public String colorId() {
        return colorId;
    }

    public String colorLabel() {
        return colorLabel;
    }

    public String size() {
        return size;
    }

    public int quantity() {
        return quantity;
    }

    public Money unitPrice() {
        return unitPrice;
    }

    public Money listPrice() {
        return listPrice;
    }
}

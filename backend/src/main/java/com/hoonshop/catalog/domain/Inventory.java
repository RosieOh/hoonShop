package com.hoonshop.catalog.domain;

import com.hoonshop.common.domain.AggregateRoot;
import com.hoonshop.common.domain.DomainException;
import jakarta.persistence.*;

/**
 * 재고 애그리거트 루트.
 *
 * <p>상품과 분리된 이유는 {@link Product} 주석 참고. 여기서 지켜야 하는 불변식은 하나뿐입니다:
 * <b>재고는 0 미만이 될 수 없다.</b>
 *
 * <p>동시성은 두 겹으로 막습니다.
 * <ol>
 *   <li>차감 시 비관적 락 — 같은 상품을 동시에 결제하면 한 트랜잭션이 기다립니다.</li>
 *   <li>{@code @Version} 낙관적 락 — 락을 빠뜨린 경로가 생겨도 갱신 분실을 잡아냅니다.</li>
 * </ol>
 * 하나만 두면 나중에 누군가 락 없는 조회로 차감하는 코드를 추가했을 때 조용히 뚫립니다.
 */
@Entity
@Table(name = "tbl_inventory")
public class Inventory extends AggregateRoot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Embedded
    private ProductCode productCode;

    @Column(nullable = false)
    private int quantity;

    @Version
    private Long version;

    protected Inventory() {
    }

    private Inventory(ProductCode productCode, int quantity) {
        if (quantity < 0) {
            throw new IllegalArgumentException("재고는 음수로 시작할 수 없습니다: " + quantity);
        }
        this.productCode = productCode;
        this.quantity = quantity;
    }

    public static Inventory of(ProductCode productCode, int quantity) {
        return new Inventory(productCode, quantity);
    }

    public boolean canFulfill(int requested) {
        return quantity >= requested;
    }

    /**
     * 재고 차감. 결제 승인 시점에만 호출합니다.
     *
     * <p>주문서 작성 시점에 미리 빼두면(선점) 결제를 포기한 장바구니가 재고를 물고 있게 되고,
     * 반환 타이머·만료 처리 같은 부수 장치가 줄줄이 필요해집니다. 이 규모에서는
     * 승인 시점 차감이 맞습니다.
     */
    public void deduct(int requested) {
        if (requested <= 0) {
            throw new IllegalArgumentException("차감 수량은 1 이상이어야 합니다: " + requested);
        }
        if (quantity < requested) {
            throw new DomainException.Conflict("OUT_OF_STOCK",
                    "재고가 부족합니다. 남은 수량: %d개".formatted(quantity));
        }
        quantity -= requested;
        if (quantity == 0) {
            registerEvent(new StockDepleted(productCode));
        }
    }

    /** 주문 취소·반품 시 되돌립니다. */
    public void restore(int amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("복원 수량은 1 이상이어야 합니다: " + amount);
        }
        quantity += amount;
    }

    /** 관리자가 실사 결과로 덮어씁니다. */
    public void adjustTo(int newQuantity) {
        if (newQuantity < 0 || newQuantity > 9999) {
            throw new DomainException.Conflict("INVALID_STOCK",
                    "재고는 0~9999 사이여야 합니다: " + newQuantity);
        }
        this.quantity = newQuantity;
    }

    public ProductCode productCode() {
        return productCode;
    }

    public int quantity() {
        return quantity;
    }

    public boolean isSoldOut() {
        return quantity == 0;
    }
}

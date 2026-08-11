package com.hoonshop.catalog.domain;

import com.hoonshop.common.domain.AggregateRoot;
import com.hoonshop.common.domain.DomainException;
import com.hoonshop.common.domain.Money;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 상품 애그리거트 루트.
 *
 * <p><b>재고를 여기에 두지 않은 이유.</b> 재고는 주문이 들어올 때마다 바뀌지만 상품 정보는
 * 거의 바뀌지 않습니다. 한 애그리거트로 묶으면 재고 차감 때마다 상품 행에 락이 걸려
 * 카탈로그 조회까지 줄을 서게 됩니다. 애그리거트 경계는 "함께 지켜야 하는 불변식"으로
 * 정하는 것이지 "같이 보이는 화면"으로 정하는 게 아니라서, 재고는 {@link Inventory}로
 * 분리하고 상품 코드로만 연결합니다.
 */
@Entity
@Table(name = "product")
public class Product extends AggregateRoot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Embedded
    private ProductCode code;

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Category category;

    @Column(nullable = false, length = 32)
    private String colorway;

    @Column(nullable = false, length = 500)
    private String description;

    @AttributeOverride(name = "amount", column = @Column(name = "list_price", nullable = false))
    private Money listPrice;

    /** 0이면 할인 없음. 판매가는 저장하지 않고 항상 파생 계산합니다. */
    @Column(nullable = false)
    private int discountRate;

    @Column(nullable = false)
    private double rating;

    @Column(nullable = false)
    private int reviewCount;

    @Column(nullable = false)
    private int soldCount;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "product_palette", joinColumns = @JoinColumn(name = "product_id"))
    @Column(name = "hex", nullable = false, length = 7)
    @OrderColumn(name = "line_order")
    private List<String> palette = new java.util.ArrayList<>();

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "product_material", joinColumns = @JoinColumn(name = "product_id"))
    @Column(name = "material", nullable = false, length = 60)
    @OrderColumn(name = "line_order")
    private List<String> materials = new java.util.ArrayList<>();

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "product_size", joinColumns = @JoinColumn(name = "product_id"))
    @Column(name = "size_label", nullable = false, length = 40)
    @OrderColumn(name = "line_order")
    private List<String> sizes = new java.util.ArrayList<>();

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "product_color_option", joinColumns = @JoinColumn(name = "product_id"))
    @OrderColumn(name = "line_order")
    private List<ColorOption> colorOptions = new java.util.ArrayList<>();

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "product_badge", joinColumns = @JoinColumn(name = "product_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "badge", nullable = false, length = 20)
    private Set<Badge> badges = new LinkedHashSet<>();

    @Column(nullable = false)
    private Instant createdAt;

    protected Product() {
    }

    private Product(ProductCode code, String name, Category category, String colorway,
                    String description, Money listPrice, int discountRate, Instant createdAt) {
        if (listPrice.isZero()) {
            throw new IllegalArgumentException("판매 상품의 정가는 0원일 수 없습니다.");
        }
        this.code = code;
        this.name = name;
        this.category = category;
        this.colorway = colorway;
        this.description = description;
        this.listPrice = listPrice;
        this.discountRate = validateRate(discountRate);
        this.createdAt = createdAt;
    }

    public static Product create(ProductCode code, String name, Category category, String colorway,
                                 String description, Money listPrice, int discountRate,
                                 Instant createdAt) {
        return new Product(code, name, category, colorway, description, listPrice, discountRate,
                createdAt);
    }

    private static int validateRate(int rate) {
        if (rate < 0 || rate >= 100) {
            throw new IllegalArgumentException("할인율은 0 이상 100 미만이어야 합니다: " + rate);
        }
        return rate;
    }

    /**
     * 실제 판매가.
     *
     * <p>계산해서 저장하지 않고 매번 파생합니다. 저장해두면 정가나 할인율을 바꿨을 때
     * 갱신을 빠뜨린 행이 생기고, 그 순간부터 잘못된 금액으로 결제됩니다.
     *
     * <p>100원 미만은 절사합니다 — 27,200원처럼 떨어지는 게 32,000원의 15%인
     * 27,200원보다 읽기 좋고, 프론트에 이미 같은 규칙이 적용돼 있습니다.
     */
    public Money sellingPrice() {
        if (discountRate == 0) {
            return listPrice;
        }
        long discounted = listPrice.minus(listPrice.percentage(discountRate)).value();
        return Money.won(Math.round(discounted / 100.0) * 100);
    }

    public Money discountAmount() {
        return listPrice.minus(sellingPrice());
    }

    /**
     * 주문에 담긴 옵션이 이 상품에 실제로 존재하는지 검증합니다.
     *
     * <p>프론트가 보낸 옵션을 그대로 믿으면 판매하지 않는 색/사이즈로 주문이 들어옵니다.
     */
    public void validateOption(String colorId, String size) {
        boolean colorExists = colorOptions.stream().anyMatch(c -> c.id().equals(colorId));
        if (!colorExists) {
            throw new DomainException.Conflict("INVALID_OPTION",
                    "%s에 없는 컬러입니다: %s".formatted(name, colorId));
        }
        if (sizes.isEmpty()) {
            return; // 사이즈가 없는 상품(귀걸이·스트랩)은 사이즈를 받지 않습니다.
        }
        if (size == null || !sizes.contains(size)) {
            throw new DomainException.Conflict("INVALID_OPTION",
                    "%s에 없는 사이즈입니다: %s".formatted(name, size));
        }
    }

    public void changePricing(Money listPrice, int discountRate) {
        if (listPrice.isZero()) {
            throw new IllegalArgumentException("판매 상품의 정가는 0원일 수 없습니다.");
        }
        this.listPrice = listPrice;
        this.discountRate = validateRate(discountRate);
    }

    public void recordSale(int quantity) {
        this.soldCount += quantity;
    }

    /* ---------------------------------------------------------- 초기 구성 --- */

    public Product withPalette(List<String> palette) {
        this.palette = new java.util.ArrayList<>(palette);
        return this;
    }

    public Product withMaterials(List<String> materials) {
        this.materials = new java.util.ArrayList<>(materials);
        return this;
    }

    public Product withSizes(List<String> sizes) {
        this.sizes = new java.util.ArrayList<>(sizes);
        return this;
    }

    public Product withColorOptions(List<ColorOption> options) {
        if (options.isEmpty()) {
            throw new IllegalArgumentException("컬러 옵션은 최소 1개 필요합니다.");
        }
        this.colorOptions = new java.util.ArrayList<>(options);
        return this;
    }

    public Product withBadges(Set<Badge> badges) {
        this.badges = new LinkedHashSet<>(badges);
        return this;
    }

    public Product withReputation(double rating, int reviewCount, int soldCount) {
        this.rating = rating;
        this.reviewCount = reviewCount;
        this.soldCount = soldCount;
        return this;
    }

    /* ------------------------------------------------------------ 접근자 --- */

    public Long id() {
        return id;
    }

    public ProductCode code() {
        return code;
    }

    public String name() {
        return name;
    }

    public Category category() {
        return category;
    }

    public String colorway() {
        return colorway;
    }

    public String description() {
        return description;
    }

    public Money listPrice() {
        return listPrice;
    }

    public int discountRate() {
        return discountRate;
    }

    public double rating() {
        return rating;
    }

    public int reviewCount() {
        return reviewCount;
    }

    public int soldCount() {
        return soldCount;
    }

    public List<String> palette() {
        return List.copyOf(palette);
    }

    public List<String> materials() {
        return List.copyOf(materials);
    }

    public List<String> sizes() {
        return List.copyOf(sizes);
    }

    public List<ColorOption> colorOptions() {
        return List.copyOf(colorOptions);
    }

    public Set<Badge> badges() {
        return Set.copyOf(badges);
    }

    public Instant createdAt() {
        return createdAt;
    }
}

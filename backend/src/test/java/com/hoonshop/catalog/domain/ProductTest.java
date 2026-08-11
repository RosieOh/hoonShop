package com.hoonshop.catalog.domain;

import com.hoonshop.common.domain.DomainException;
import com.hoonshop.common.domain.Money;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Product")
class ProductTest {

    private Product necklace(long price, int rate) {
        return Product.create(ProductCode.of("P0001"), "오후 세 시의 버터", Category.NECKLACE,
                        "butter", "설명", Money.won(price), rate, Instant.now())
                .withColorOptions(List.of(
                        ColorOption.of("butter", "버터크림", "#F6D89B"),
                        ColorOption.of("lilac", "라일락", "#C4B0DE")))
                .withSizes(List.of("38cm (초커)", "42cm (기본)"));
    }

    @Test
    @DisplayName("판매가는 100원 단위로 반올림한다")
    void roundsSellingPriceToHundred() {
        // 32,000 × 85% = 27,200
        assertThat(necklace(32_000, 15).sellingPrice()).isEqualTo(Money.won(27_200));
        // 29,900 × 85% = 25,415 → 25,400
        assertThat(necklace(29_900, 15).sellingPrice()).isEqualTo(Money.won(25_400));
    }

    @Test
    @DisplayName("할인율이 0이면 정가가 그대로 판매가다")
    void noDiscountKeepsListPrice() {
        assertThat(necklace(46_000, 0).sellingPrice()).isEqualTo(Money.won(46_000));
    }

    @Test
    @DisplayName("정가 0원인 상품은 만들 수 없다")
    void rejectsZeroPrice() {
        assertThatThrownBy(() -> Product.create(ProductCode.of("P0002"), "x", Category.RING,
                "mint", "설명", Money.ZERO, 0, Instant.now()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("취급하지 않는 컬러로 주문하면 거부한다")
    void rejectsUnknownColor() {
        assertThatThrownBy(() -> necklace(32_000, 0).validateOption("neon", "42cm (기본)"))
                .isInstanceOf(DomainException.Conflict.class)
                .hasMessageContaining("없는 컬러");
    }

    @Test
    @DisplayName("사이즈가 있는 상품인데 사이즈를 안 고르면 거부한다")
    void rejectsMissingSize() {
        assertThatThrownBy(() -> necklace(32_000, 0).validateOption("butter", null))
                .isInstanceOf(DomainException.Conflict.class)
                .hasMessageContaining("없는 사이즈");
    }

    @Test
    @DisplayName("사이즈가 없는 상품(귀걸이)은 사이즈를 요구하지 않는다")
    void allowsNullSizeWhenProductHasNoSizes() {
        Product earring = Product.create(ProductCode.of("P0011"), "버터 드롭 이어링",
                        Category.EARRING, "butter", "설명", Money.won(26_000), 0, Instant.now())
                .withColorOptions(List.of(ColorOption.of("butter", "버터크림", "#F6D89B")));

        earring.validateOption("butter", null); // 예외가 나지 않아야 한다
    }

    @Test
    @DisplayName("상품 코드 형식이 어긋나면 만들 수 없다")
    void rejectsMalformedCode() {
        assertThatThrownBy(() -> ProductCode.of("X1"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}

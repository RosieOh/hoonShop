package com.hoonshop.catalog.domain;

import com.hoonshop.common.domain.DomainException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Inventory")
class InventoryTest {

    private Inventory withStock(int quantity) {
        return Inventory.of(ProductCode.of("P0001"), quantity);
    }

    @Test
    @DisplayName("재고보다 많이 차감하면 거부한다 — 마이너스 재고는 존재할 수 없다")
    void rejectsOverDeduction() {
        Inventory inventory = withStock(2);

        assertThatThrownBy(() -> inventory.deduct(3))
                .isInstanceOf(DomainException.Conflict.class)
                .hasMessageContaining("남은 수량: 2개");

        assertThat(inventory.quantity()).isEqualTo(2); // 실패해도 상태가 바뀌지 않는다
    }

    @Test
    @DisplayName("정확히 남은 만큼 차감하면 품절이 되고 이벤트가 등록된다")
    void publishesEventWhenDepleted() {
        Inventory inventory = withStock(3);

        inventory.deduct(3);

        assertThat(inventory.quantity()).isZero();
        assertThat(inventory.isSoldOut()).isTrue();
        assertThat(inventory.domainEvents()).hasSize(1)
                .first().isInstanceOf(StockDepleted.class);
    }

    @Test
    @DisplayName("품절되지 않으면 이벤트를 만들지 않는다")
    void noEventWhenStockRemains() {
        Inventory inventory = withStock(10);
        inventory.deduct(3);
        assertThat(inventory.domainEvents()).isEmpty();
    }

    @Test
    @DisplayName("이벤트는 한 번만 수거된다 — 두 번 발행하면 재고가 두 번 빠진다")
    void pollClearsEvents() {
        Inventory inventory = withStock(1);
        inventory.deduct(1);

        assertThat(inventory.pollEvents()).hasSize(1);
        assertThat(inventory.pollEvents()).isEmpty();
    }

    @Test
    @DisplayName("취소로 복원하면 다시 판매 가능해진다")
    void restoresStock() {
        Inventory inventory = withStock(0);
        inventory.restore(5);
        assertThat(inventory.canFulfill(5)).isTrue();
    }

    @Test
    @DisplayName("관리자 재고 조정은 0~9999 범위만 허용한다")
    void rejectsOutOfRangeAdjustment() {
        Inventory inventory = withStock(10);
        assertThatThrownBy(() -> inventory.adjustTo(10_000))
                .isInstanceOf(DomainException.Conflict.class);
    }
}

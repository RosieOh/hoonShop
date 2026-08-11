package com.hoonshop.common.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Money")
class MoneyTest {

    @Test
    @DisplayName("음수 금액은 만들 수 없다")
    void rejectsNegative() {
        assertThatThrownBy(() -> Money.won(-1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Nested
    @DisplayName("뺄셈")
    class Subtraction {

        @Test
        @DisplayName("원금액보다 큰 금액을 빼면 예외 — 조용히 0이 되면 잘못된 청구를 놓친다")
        void throwsWhenResultWouldBeNegative() {
            assertThatThrownBy(() -> Money.won(1_000).minus(Money.won(1_001)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("차감 금액이 원금액보다 큽니다");
        }

        @Test
        @DisplayName("minusOrZero는 0에서 자른다 — 할인 상한 계산 전용")
        void clampsToZero() {
            assertThat(Money.won(1_000).minusOrZero(Money.won(5_000))).isEqualTo(Money.ZERO);
        }
    }

    @Test
    @DisplayName("퍼센트 할인은 원 단위 미만을 버린다")
    void percentageTruncates() {
        // 33,333 × 15% = 4,999.95 → 4,999
        assertThat(Money.won(33_333).percentage(15)).isEqualTo(Money.won(4_999));
    }

    @Test
    @DisplayName("할인율은 0~100 범위만 허용한다")
    void rejectsInvalidPercentage() {
        assertThatThrownBy(() -> Money.won(1_000).percentage(101))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("같은 금액이면 동등하다 (값 객체)")
    void valueEquality() {
        assertThat(Money.won(5_000)).isEqualTo(Money.won(5_000));
        assertThat(Money.won(5_000)).hasSameHashCodeAs(Money.won(5_000));
    }
}

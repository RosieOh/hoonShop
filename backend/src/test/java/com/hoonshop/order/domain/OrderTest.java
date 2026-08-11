package com.hoonshop.order.domain;

import com.hoonshop.common.domain.DomainException;
import com.hoonshop.common.domain.Money;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Order")
class OrderTest {

    private static final ShippingAddress ADDRESS = ShippingAddress.of(
            "김태훈", "010-2345-6789", "04524", "서울특별시 중구 세종대로 110", "3층 302호");

    private OrderLineSpec line(long listPrice, long unitPrice, int quantity) {
        return new OrderLineSpec("P0001", "오후 세 시의 버터", "butter", "버터크림",
                "42cm (기본)", quantity, Money.won(listPrice), Money.won(unitPrice));
    }

    private Order place(List<OrderLineSpec> lines, Money couponDiscount, Money shippingDiscount) {
        return Order.place(OrderNumber.issue(1), "hoon@example.com", "김태훈", lines, ADDRESS,
                "문 앞에 놓아주세요", List.of(), couponDiscount, shippingDiscount);
    }

    @Nested
    @DisplayName("금액 계산")
    class Amounts {

        @Test
        @DisplayName("5만원 미만이면 배송비 3,000원이 붙는다")
        void chargesShippingBelowThreshold() {
            Order order = place(List.of(line(32_000, 27_200, 1)), Money.ZERO, Money.ZERO);

            assertThat(order.amounts().itemsListTotal()).isEqualTo(Money.won(32_000));
            assertThat(order.amounts().itemDiscount()).isEqualTo(Money.won(4_800));
            assertThat(order.amounts().shippingFee()).isEqualTo(Money.won(3_000));
            assertThat(order.amounts().payable()).isEqualTo(Money.won(30_200));
        }

        @Test
        @DisplayName("판매가 합계가 5만원 이상이면 배송비가 없다 — 기준은 정가가 아니라 판매가")
        void freeShippingUsesDiscountedTotal() {
            Order order = place(List.of(line(46_000, 46_000, 2)), Money.ZERO, Money.ZERO);

            assertThat(order.amounts().shippingFee()).isEqualTo(Money.ZERO);
            assertThat(order.amounts().payable()).isEqualTo(Money.won(92_000));
        }

        @Test
        @DisplayName("쿠폰 할인은 상품 금액에서만 빠지고 배송비를 침범하지 않는다")
        void couponDoesNotEatShipping() {
            Order order = place(List.of(line(20_000, 20_000, 1)), Money.won(5_000), Money.ZERO);

            // 20,000 - 5,000 + 3,000 = 18,000
            assertThat(order.amounts().payable()).isEqualTo(Money.won(18_000));
        }

        @Test
        @DisplayName("배송비 쿠폰은 배송비까지만 깎는다")
        void shippingCouponCappedAtShippingFee() {
            Order order = place(List.of(line(20_000, 20_000, 1)), Money.ZERO, Money.won(9_999));

            assertThat(order.amounts().shippingDiscount()).isEqualTo(Money.won(3_000));
            assertThat(order.amounts().payable()).isEqualTo(Money.won(20_000));
        }

        @Test
        @DisplayName("항목이 없는 주문은 만들 수 없다")
        void rejectsEmptyOrder() {
            assertThatThrownBy(() -> place(List.of(), Money.ZERO, Money.ZERO))
                    .isInstanceOf(DomainException.Conflict.class)
                    .hasMessageContaining("주문할 상품이 없습니다");
        }
    }

    @Nested
    @DisplayName("금액 대조")
    class AmountVerification {

        @Test
        @DisplayName("클라이언트가 보낸 금액이 다르면 결제로 넘어가지 않는다")
        void rejectsMismatchedAmount() {
            Order order = place(List.of(line(32_000, 27_200, 1)), Money.ZERO, Money.ZERO);

            assertThatThrownBy(() -> order.assertPayableMatches(Money.won(1_000)))
                    .isInstanceOf(DomainException.Conflict.class)
                    .hasMessageContaining("서버 계산과 다릅니다");
        }

        @Test
        @DisplayName("일치하면 통과한다")
        void acceptsMatchingAmount() {
            Order order = place(List.of(line(32_000, 27_200, 1)), Money.ZERO, Money.ZERO);
            order.assertPayableMatches(Money.won(30_200));
        }
    }

    @Nested
    @DisplayName("상태 전이")
    class StatusTransition {

        @Test
        @DisplayName("결제 완료 → 제작 중 → 발송 → 배송 완료 순서로만 진행된다")
        void followsHappyPath() {
            Order order = place(List.of(line(32_000, 32_000, 1)), Money.ZERO, Money.ZERO);

            order.markPaid();
            assertThat(order.status()).isEqualTo(OrderStatus.PAID);

            order.changeStatus(OrderStatus.MAKING);
            order.changeStatus(OrderStatus.SHIPPED);
            order.changeStatus(OrderStatus.DELIVERED);

            assertThat(order.status()).isEqualTo(OrderStatus.DELIVERED);
        }

        @Test
        @DisplayName("단계를 건너뛸 수 없다")
        void rejectsSkippingSteps() {
            Order order = place(List.of(line(32_000, 32_000, 1)), Money.ZERO, Money.ZERO);
            order.markPaid();

            assertThatThrownBy(() -> order.changeStatus(OrderStatus.SHIPPED))
                    .isInstanceOf(DomainException.Conflict.class)
                    .hasMessageContaining("바꿀 수 없습니다");
        }

        @Test
        @DisplayName("배송 완료된 주문은 취소할 수 없다")
        void rejectsCancellingDeliveredOrder() {
            Order order = place(List.of(line(32_000, 32_000, 1)), Money.ZERO, Money.ZERO);
            order.markPaid();
            order.changeStatus(OrderStatus.MAKING);
            order.changeStatus(OrderStatus.SHIPPED);
            order.changeStatus(OrderStatus.DELIVERED);

            assertThatThrownBy(() -> order.cancel("변심"))
                    .isInstanceOf(DomainException.Conflict.class)
                    .hasMessageContaining("이미 종료된 주문");
        }

        @Test
        @DisplayName("결제 완료 시 재고 차감 이벤트가 나간다")
        void publishesOrderPaidWithStockChanges() {
            Order order = place(List.of(line(32_000, 32_000, 2)), Money.ZERO, Money.ZERO);
            order.pollEvents(); // 생성 이벤트 비우기

            order.markPaid();

            assertThat(order.domainEvents()).hasSize(1);
            OrderPaid event = (OrderPaid) order.domainEvents().get(0);
            assertThat(event.stockChanges())
                    .containsExactly(new OrderPaid.StockChange("P0001", 2));
        }

        @Test
        @DisplayName("결제 전 취소는 wasPaid=false — 재고를 되돌리면 안 된다")
        void cancellationBeforePaymentDoesNotRestoreStock() {
            Order order = place(List.of(line(32_000, 32_000, 1)), Money.ZERO, Money.ZERO);
            order.pollEvents();

            order.cancel("변심");

            OrderCancelled event = (OrderCancelled) order.domainEvents().get(0);
            assertThat(event.wasPaid()).isFalse();
        }

        @Test
        @DisplayName("결제 후 취소는 wasPaid=true — 재고를 되돌려야 한다")
        void cancellationAfterPaymentRestoresStock() {
            Order order = place(List.of(line(32_000, 32_000, 1)), Money.ZERO, Money.ZERO);
            order.markPaid();
            order.pollEvents();

            order.cancel("품절");

            OrderCancelled event = (OrderCancelled) order.domainEvents().get(0);
            assertThat(event.wasPaid()).isTrue();
        }
    }

    @Nested
    @DisplayName("배송지")
    class Address {

        @Test
        @DisplayName("휴대폰 번호 형식이 아니면 거부한다")
        void rejectsInvalidPhone() {
            assertThatThrownBy(() -> ShippingAddress.of("김태훈", "02-123-4567", "04524",
                    "서울시", ""))
                    .isInstanceOf(DomainException.class)
                    .hasMessageContaining("휴대폰 번호");
        }

        @Test
        @DisplayName("우편번호는 5자리 숫자여야 한다")
        void rejectsInvalidZipcode() {
            assertThatThrownBy(() -> ShippingAddress.of("김태훈", "010-2345-6789", "123",
                    "서울시", ""))
                    .isInstanceOf(DomainException.class)
                    .hasMessageContaining("우편번호");
        }
    }

    @Test
    @DisplayName("다른 사람의 주문은 소유자로 인정되지 않는다")
    void ownershipCheck() {
        Order order = place(List.of(line(32_000, 32_000, 1)), Money.ZERO, Money.ZERO);

        assertThat(order.isOwnedBy("hoon@example.com")).isTrue();
        assertThat(order.isOwnedBy("someone@example.com")).isFalse();
    }
}

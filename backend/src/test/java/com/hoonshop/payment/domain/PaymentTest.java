package com.hoonshop.payment.domain;

import com.hoonshop.common.domain.DomainException;
import com.hoonshop.common.domain.Money;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Payment")
class PaymentTest {

    private static final Money AMOUNT = Money.won(30_200);

    private Payment requested() {
        return Payment.request("key-1", "ORD-2026-00001", "hoon@example.com", AMOUNT);
    }

    private PaymentGateway.Approval approvalOf(Money amount) {
        return new PaymentGateway.Approval("PAY_ABC", PaymentMethod.CARD, amount,
                PaymentStatus.APPROVED, Instant.now(), "https://receipt", null);
    }

    @Test
    @DisplayName("0원 결제는 만들 수 없다")
    void rejectsZeroAmount() {
        assertThatThrownBy(() -> Payment.request("k", "ORD", "a@b.com", Money.ZERO))
                .isInstanceOf(DomainException.Conflict.class);
    }

    @Test
    @DisplayName("생성 시점에 원장에 기록이 남는다 — PG 호출 전에 흔적이 있어야 한다")
    void recordsLedgerOnRequest() {
        assertThat(requested().ledger()).hasSize(1)
                .first()
                .extracting(PaymentLedgerEntry::entryType).isEqualTo("REQUESTED");
    }

    @Nested
    @DisplayName("승인")
    class Approve {

        @Test
        @DisplayName("승인 금액이 요청 금액과 다르면 인정하지 않는다")
        void rejectsAmountMismatch() {
            Payment payment = requested();

            assertThatThrownBy(() -> payment.approve(approvalOf(Money.won(31_200))))
                    .isInstanceOf(DomainException.Conflict.class)
                    .hasMessageContaining("주문 금액과 다릅니다");

            assertThat(payment.status()).isEqualTo(PaymentStatus.REQUESTED);
            assertThat(payment.ledger())
                    .extracting(PaymentLedgerEntry::entryType)
                    .contains("AMOUNT_MISMATCH");
        }

        @Test
        @DisplayName("정상 승인 시 PaymentApproved 이벤트가 나간다")
        void publishesApprovedEvent() {
            Payment payment = requested();
            payment.approve(approvalOf(AMOUNT));

            assertThat(payment.status()).isEqualTo(PaymentStatus.APPROVED);
            assertThat(payment.domainEvents()).hasSize(1)
                    .first().isInstanceOf(PaymentApproved.class);
        }

        @Test
        @DisplayName("같은 승인이 두 번 들어와도 상태가 흔들리지 않는다 (멱등)")
        void approveIsIdempotent() {
            Payment payment = requested();
            payment.approve(approvalOf(AMOUNT));
            payment.pollEvents();

            payment.approve(approvalOf(AMOUNT));

            assertThat(payment.status()).isEqualTo(PaymentStatus.APPROVED);
            assertThat(payment.domainEvents()).isEmpty(); // 두 번째는 이벤트를 만들지 않는다
        }
    }

    @Nested
    @DisplayName("결과 미확인")
    class Unknown {

        @Test
        @DisplayName("타임아웃은 실패가 아니라 UNKNOWN — 승인됐을 수 있다")
        void marksUnknownNotFailed() {
            Payment payment = requested();
            payment.markUnknown("PG 응답 시간 초과");

            assertThat(payment.status()).isEqualTo(PaymentStatus.UNKNOWN);
            assertThat(payment.status().needsReconciliation()).isTrue();
            assertThat(payment.domainEvents()).hasSize(1)
                    .first().isInstanceOf(PaymentNeedsReconciliation.class);
        }

        @Test
        @DisplayName("대사로 승인이 확인되면 정상 승인 처리된다")
        void reconcilesToApproved() {
            Payment payment = requested();
            payment.markUnknown("타임아웃");
            payment.pollEvents();

            payment.resolveByReconciliation(approvalOf(AMOUNT));

            assertThat(payment.status()).isEqualTo(PaymentStatus.APPROVED);
            assertThat(payment.domainEvents()).hasSize(1)
                    .first().isInstanceOf(PaymentApproved.class);
            assertThat(payment.ledger())
                    .extracting(PaymentLedgerEntry::entryType)
                    .contains("UNKNOWN", "RECONCILED", "APPROVED");
        }

        @Test
        @DisplayName("PG를 부르기 직전에 멈춘 REQUESTED 건도 대사로 확정할 수 있다")
        void reconcilesStuckRequested() {
            Payment payment = requested();
            payment.resolveAsFailed("결제창에서 이탈");
            assertThat(payment.status()).isEqualTo(PaymentStatus.FAILED);
        }
    }

    @Nested
    @DisplayName("가상계좌")
    class VirtualAccount {

        private PaymentGateway.Approval waiting() {
            return new PaymentGateway.Approval("PAY_VA", PaymentMethod.VIRTUAL, AMOUNT,
                    PaymentStatus.WAITING_FOR_DEPOSIT, Instant.now(), null,
                    new PaymentGateway.VirtualAccount("신한", "110-123-456789", "훈샵",
                            Instant.now().plus(3, ChronoUnit.DAYS)));
        }

        @Test
        @DisplayName("발급 시점에는 결제 완료가 아니다 — 아직 돈이 들어오지 않았다")
        void issuingDoesNotCompletePayment() {
            Payment payment = requested();
            payment.approve(waiting());

            assertThat(payment.status()).isEqualTo(PaymentStatus.WAITING_FOR_DEPOSIT);
            assertThat(payment.virtualAccountNumber()).isEqualTo("110-123-456789");
            // 주문을 결제 완료로 만드는 이벤트가 아직 나가면 안 됩니다.
            assertThat(payment.domainEvents()).isEmpty();
        }

        @Test
        @DisplayName("입금이 확인되면 그때 PaymentApproved가 나간다")
        void depositCompletesPayment() {
            Payment payment = requested();
            payment.approve(waiting());

            payment.confirmDeposit(AMOUNT);

            assertThat(payment.status()).isEqualTo(PaymentStatus.APPROVED);
            assertThat(payment.domainEvents()).hasSize(1)
                    .first().isInstanceOf(PaymentApproved.class);
        }

        @Test
        @DisplayName("입금 금액이 다르면 완료 처리하지 않는다")
        void rejectsWrongDepositAmount() {
            Payment payment = requested();
            payment.approve(waiting());

            assertThatThrownBy(() -> payment.confirmDeposit(Money.won(10_000)))
                    .isInstanceOf(DomainException.Conflict.class)
                    .hasMessageContaining("입금 금액이 주문 금액과 다릅니다");
        }

        @Test
        @DisplayName("웹훅이 두 번 와도 한 번만 처리된다 (멱등)")
        void depositIsIdempotent() {
            Payment payment = requested();
            payment.approve(waiting());
            payment.confirmDeposit(AMOUNT);
            payment.pollEvents();

            payment.confirmDeposit(AMOUNT);

            assertThat(payment.domainEvents()).isEmpty();
        }
    }

    @Nested
    @DisplayName("취소")
    class Cancel {

        private Payment approved() {
            Payment payment = requested();
            payment.approve(approvalOf(AMOUNT));
            payment.pollEvents();
            return payment;
        }

        @Test
        @DisplayName("전액 취소")
        void fullCancel() {
            Payment payment = approved();
            payment.cancel(AMOUNT, "고객 요청");

            assertThat(payment.status()).isEqualTo(PaymentStatus.CANCELLED);
            assertThat(payment.remainingAmount()).isEqualTo(Money.ZERO);

            PaymentCancelled event = (PaymentCancelled) payment.domainEvents().get(0);
            assertThat(event.full()).isTrue();
        }

        @Test
        @DisplayName("부분 취소 후 남은 금액이 계산된다")
        void partialCancel() {
            Payment payment = approved();
            payment.cancel(Money.won(10_000), "일부 품절");

            assertThat(payment.status()).isEqualTo(PaymentStatus.PARTIAL_CANCELLED);
            assertThat(payment.remainingAmount()).isEqualTo(Money.won(20_200));

            PaymentCancelled event = (PaymentCancelled) payment.domainEvents().get(0);
            assertThat(event.full()).isFalse();
        }

        @Test
        @DisplayName("남은 금액을 넘겨 취소할 수 없다")
        void rejectsOverCancel() {
            Payment payment = approved();
            payment.cancel(Money.won(20_000), "1차");

            assertThatThrownBy(() -> payment.cancel(Money.won(20_000), "2차"))
                    .isInstanceOf(DomainException.Conflict.class)
                    .hasMessageContaining("취소 가능 금액을 초과");
        }

        @Test
        @DisplayName("승인되지 않은 결제는 취소할 수 없다")
        void rejectsCancellingUnapproved() {
            assertThatThrownBy(() -> requested().cancel(AMOUNT, "x"))
                    .isInstanceOf(DomainException.Conflict.class)
                    .hasMessageContaining("취소할 수 없는 상태");
        }

        @Test
        @DisplayName("취소 이력이 원장에 남는다")
        void recordsCancellationInLedger() {
            Payment payment = approved();
            payment.cancel(Money.won(10_000), "일부 품절");
            payment.cancel(Money.won(20_200), "나머지 취소");

            assertThat(payment.ledger())
                    .extracting(PaymentLedgerEntry::entryType)
                    .containsSubsequence("REQUESTED", "APPROVED", "PARTIAL_CANCELLED", "CANCELLED");
        }
    }
}

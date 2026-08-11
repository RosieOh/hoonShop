package com.hoonshop.payment.presentation;

import com.hoonshop.common.domain.DomainException;
import com.hoonshop.payment.application.ConfirmPaymentService;
import com.hoonshop.payment.domain.PaymentMethod;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Payment", description = "결제")
@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final ConfirmPaymentService confirmPaymentService;

    public PaymentController(ConfirmPaymentService confirmPaymentService) {
        this.confirmPaymentService = confirmPaymentService;
    }

    @Operation(summary = "결제 승인",
            description = """
                    금액은 요청 본문이 아니라 주문에 기록된 값으로 승인합니다.
                    Idempotency-Key 헤더를 보내면 네트워크 재시도로 인한 이중 결제를 막습니다.
                    """)
    @PostMapping("/confirm")
    public ConfirmPaymentService.Receipt confirm(
            @RequestBody @Valid ConfirmRequest request,
            @Parameter(description = "재시도 시 같은 값을 보내면 한 번만 청구됩니다")
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            Authentication authentication) {

        if (authentication == null) {
            throw new DomainException.Unauthorized("UNAUTHENTICATED", "로그인이 필요합니다.");
        }

        // 헤더가 없으면 주문번호로 대체합니다. 한 주문은 어차피 한 번만 결제되어야 하므로
        // 최소한의 멱등성은 이것만으로도 확보됩니다.
        String key = (idempotencyKey == null || idempotencyKey.isBlank())
                ? "order:" + request.orderId()
                : idempotencyKey;

        var command = new ConfirmPaymentService.ConfirmPaymentCommand(
                key,
                request.orderId(),
                authentication.getName(),
                PaymentMethod.valueOf(request.method()),
                request.paymentKey(),
                request.card() == null ? null : request.card().number());

        return confirmPaymentService.confirm(command);
    }

    public record ConfirmRequest(@NotBlank String orderId, @NotBlank String method,
                                 String paymentKey, Card card) {

        /** 데모 전용. 실연동에서는 카드 정보가 서버로 오지 않습니다. */
        public record Card(String number) {
        }
    }
}

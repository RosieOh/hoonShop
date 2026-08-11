package com.hoonshop.payment.presentation;

import com.hoonshop.common.domain.DomainException;
import com.hoonshop.payment.application.ConfirmPaymentService;
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
                    **카드 정보를 받지 않습니다.** 프론트가 PG 결제창에서 받은 paymentKey만 보냅니다.
                    카드번호가 서버를 거치는 순간 PCI-DSS 범위에 들어가므로 설계상 차단했습니다.

                    금액은 요청 본문이 아니라 주문에 기록된 값으로 승인합니다.
                    Idempotency-Key 헤더를 보내면 네트워크 재시도로 인한 이중 결제를 막습니다.
                    """)
    @PostMapping("/confirm")
    public ConfirmPaymentService.Receipt confirm(
            @RequestBody @Valid ConfirmRequest request,
            @Parameter(description = "재시도 시 같은 값을 보내면 한 번만 청구됩니다")
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            Authentication authentication) {

        String email = requireEmail(authentication);

        // 헤더가 없으면 주문번호로 대체합니다. 한 주문은 한 번만 결제되어야 하므로
        // 최소한의 멱등성은 이것만으로도 확보됩니다.
        String key = (idempotencyKey == null || idempotencyKey.isBlank())
                ? "order:" + request.orderId()
                : idempotencyKey;

        return confirmPaymentService.confirm(new ConfirmPaymentService.ConfirmPaymentCommand(
                key, request.orderId(), email, request.paymentKey()));
    }

    private String requireEmail(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new DomainException.Unauthorized("UNAUTHENTICATED", "로그인이 필요합니다.");
        }
        return authentication.getName();
    }

    /**
     * @param paymentKey PG 결제창(SDK)이 발급한 키. 카드번호가 아닙니다.
     */
    public record ConfirmRequest(@NotBlank String orderId, @NotBlank String paymentKey) {
    }
}

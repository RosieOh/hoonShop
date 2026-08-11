package com.hoonshop.payment.presentation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hoonshop.common.domain.DomainException;
import com.hoonshop.common.domain.Money;
import com.hoonshop.payment.application.PaymentTransactionService;
import com.hoonshop.payment.domain.PaymentGateway;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * PG 웹훅 수신.
 *
 * <p>가상계좌는 승인 시점에 돈이 들어오지 않습니다. 고객이 나중에 입금하면 PG가 이 엔드포인트로
 * 알려주고, 그때 비로소 주문이 결제 완료가 됩니다. 웹훅이 없으면 가상계좌 주문은 영원히
 * 입금 대기로 남습니다.
 *
 * <p>세 가지를 반드시 지킵니다.
 * <ol>
 *   <li><b>서명 검증</b> — 없으면 누구나 "입금됐다"고 우겨서 공짜로 상품을 받아갑니다.</li>
 *   <li><b>멱등 처리</b> — PG는 응답이 늦으면 같은 웹훅을 여러 번 보냅니다.
 *       애그리거트가 중복 입금 확인을 흡수합니다.</li>
 *   <li><b>항상 200 응답</b> — 우리 쪽 처리 실패로 4xx/5xx를 주면 PG가 계속 재전송합니다.
 *       실패는 로그로 남기고 응답은 200으로 돌려준 뒤, 대사로 복구합니다.</li>
 * </ol>
 */
@Tag(name = "Payment", description = "결제")
@RestController
@RequestMapping("/api/payments/webhook")
public class PaymentWebhookController {

    private static final Logger log = LoggerFactory.getLogger(PaymentWebhookController.class);

    private final PaymentGateway gateway;
    private final PaymentTransactionService tx;
    private final ObjectMapper mapper;

    public PaymentWebhookController(PaymentGateway gateway, PaymentTransactionService tx,
                                    ObjectMapper mapper) {
        this.gateway = gateway;
        this.tx = tx;
        this.mapper = mapper;
    }

    @Operation(summary = "PG 웹훅 (가상계좌 입금 등)")
    @PostMapping
    public ResponseEntity<String> receive(
            @RequestBody String payload,
            @RequestHeader(value = "TossPayments-Signature", required = false) String signature) {

        if (!gateway.verifyWebhookSignature(payload, signature)) {
            // 서명 실패는 401로 돌려줍니다 — 정상적인 PG라면 재전송하지 않습니다.
            log.warn("웹훅 서명 검증 실패 — 요청을 무시합니다.");
            return ResponseEntity.status(401).body("invalid signature");
        }

        try {
            JsonNode json = mapper.readTree(payload);
            String eventType = json.path("eventType").asText("");
            JsonNode data = json.path("data").isMissingNode() ? json : json.path("data");

            String status = data.path("status").asText("");
            String paymentKey = data.path("paymentKey").asText(null);
            long amount = data.path("totalAmount").asLong();

            if (paymentKey == null) {
                log.warn("웹훅에 paymentKey가 없습니다 — 무시. eventType={}", eventType);
                return ResponseEntity.ok("ignored");
            }

            if ("DONE".equals(status)) {
                tx.confirmDeposit(paymentKey, Money.won(amount));
                log.info("가상계좌 입금 확인 — paymentKey={} amount={}", paymentKey, amount);
            } else {
                log.info("처리 대상이 아닌 웹훅 — status={} paymentKey={}", status, paymentKey);
            }

        } catch (DomainException e) {
            // 이미 처리된 웹훅 등. 재전송을 유발하지 않도록 200으로 응답합니다.
            log.info("웹훅 처리 건너뜀 — {}", e.getMessage());
        } catch (Exception e) {
            // 여기서 5xx를 주면 PG가 무한 재전송합니다. 로그로 남기고 대사에 맡깁니다.
            log.error("웹훅 처리 실패 — 대사로 복구 필요", e);
        }

        return ResponseEntity.ok("ok");
    }
}

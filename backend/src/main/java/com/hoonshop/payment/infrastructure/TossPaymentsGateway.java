package com.hoonshop.payment.infrastructure;

import com.hoonshop.common.domain.Money;
import com.hoonshop.payment.domain.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Base64;

/**
 * 토스페이먼츠 연동.
 *
 * <p>{@code hoonshop.payment.provider=toss}일 때만 활성화됩니다. 기본값은 목입니다.
 *
 * <p>이 어댑터가 지키는 것:
 * <ul>
 *   <li><b>타임아웃</b> — 연결 3초, 응답 10초. 무한 대기는 커넥션 풀을 말립니다.</li>
 *   <li><b>타임아웃과 거절을 구분</b> — 타임아웃은 {@link GatewayTimeoutException},
 *       PG의 명시적 거절은 {@link GatewayException}. 뭉뚱그리면 승인된 결제를 놓칩니다.</li>
 *   <li><b>5xx도 미확인으로 처리</b> — 서버 오류는 승인 여부를 알 수 없다는 뜻입니다.</li>
 *   <li><b>비밀키를 로그에 남기지 않음</b></li>
 * </ul>
 *
 * <p>참고: 토스페이먼츠 승인 API는 {@code orderId}와 {@code amount}를 함께 받아
 * PG 쪽에서도 금액을 대조합니다. 우리 서버의 검증과 합쳐 이중으로 막힙니다.
 */
@Component
@ConditionalOnProperty(name = "hoonshop.payment.provider", havingValue = "toss")
public class TossPaymentsGateway implements PaymentGateway {

    private static final Logger log = LoggerFactory.getLogger(TossPaymentsGateway.class);
    private static final String API = "https://api.tosspayments.com/v1/payments";

    private final HttpClient http;
    private final ObjectMapper mapper;
    private final String authorization;
    private final String webhookSecret;
    private final Duration responseTimeout;

    public TossPaymentsGateway(ObjectMapper mapper,
                               @Value("${hoonshop.payment.toss.secret-key}") String secretKey,
                               @Value("${hoonshop.payment.toss.webhook-secret:}") String webhookSecret,
                               @Value("${hoonshop.payment.toss.timeout:PT10S}") Duration timeout) {
        this.mapper = mapper;
        this.webhookSecret = webhookSecret;
        this.responseTimeout = timeout;
        // 토스는 시크릿키를 사용자명으로 하는 Basic 인증을 씁니다(비밀번호는 빈 문자열).
        this.authorization = "Basic " + Base64.getEncoder()
                .encodeToString((secretKey + ":").getBytes(StandardCharsets.UTF_8));
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .build();
    }

    @Override
    public Approval confirm(ConfirmCommand command) {
        String body = """
                {"paymentKey":"%s","orderId":"%s","amount":%d}"""
                .formatted(command.paymentKey(), command.orderNumber(), command.amount().value());

        JsonNode response = post(API + "/confirm", body, "승인");
        return toApproval(response);
    }

    @Override
    public Approval inquire(String paymentKey) {
        JsonNode response = get(API + "/" + paymentKey);
        return toApproval(response);
    }

    @Override
    public Cancellation cancel(String paymentKey, Money cancelAmount, String reason,
                               String idempotencyKey) {
        String body = """
                {"cancelReason":"%s","cancelAmount":%d}"""
                .formatted(escape(reason), cancelAmount.value());

        JsonNode response = post(API + "/" + paymentKey + "/cancel", body, "취소", idempotencyKey);

        long balance = response.path("balanceAmount").asLong();
        return new Cancellation(paymentKey, cancelAmount, Money.won(balance), Instant.now());
    }

    /**
     * 웹훅 서명 검증 (HMAC-SHA256).
     *
     * <p>검증 없이 웹훅을 신뢰하면 누구나 "가상계좌에 입금됐다"는 요청을 보내
     * 돈을 내지 않고 상품을 받아갈 수 있습니다.
     */
    @Override
    public boolean verifyWebhookSignature(String payload, String signature) {
        if (webhookSecret.isBlank()) {
            log.warn("웹훅 시크릿이 설정되지 않았습니다 — 검증을 건너뜁니다. 운영에서는 반드시 설정하세요.");
            return false;
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(webhookSecret.getBytes(StandardCharsets.UTF_8),
                    "HmacSHA256"));
            String expected = Base64.getEncoder()
                    .encodeToString(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
            // 타이밍 공격을 막기 위해 상수 시간 비교를 씁니다.
            return java.security.MessageDigest.isEqual(
                    expected.getBytes(StandardCharsets.UTF_8),
                    signature == null ? new byte[0] : signature.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            log.error("웹훅 서명 검증 실패", e);
            return false;
        }
    }

    /* --------------------------------------------------------------- 내부 --- */

    private JsonNode post(String url, String body, String action) {
        return post(url, body, action, null);
    }

    private JsonNode post(String url, String body, String action, String idempotencyKey) {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url))
                .timeout(responseTimeout)
                .header("Authorization", authorization)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8));

        if (idempotencyKey != null) {
            builder.header("Idempotency-Key", idempotencyKey);
        }
        return send(builder.build(), action);
    }

    private JsonNode get(String url) {
        return send(HttpRequest.newBuilder(URI.create(url))
                .timeout(responseTimeout)
                .header("Authorization", authorization)
                .GET()
                .build(), "조회");
    }

    private JsonNode send(HttpRequest request, String action) {
        try {
            HttpResponse<String> response =
                    http.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            if (response.statusCode() >= 500) {
                // 서버 오류는 "승인이 됐는지 안 됐는지 모른다"는 뜻입니다.
                throw new GatewayTimeoutException(
                        "PG %s 중 서버 오류 (HTTP %d)".formatted(action, response.statusCode()), null);
            }

            JsonNode json = mapper.readTree(response.body());

            if (response.statusCode() >= 400) {
                String code = json.path("code").asText("PG_ERROR");
                String message = json.path("message").asText("결제 처리 중 오류가 발생했습니다.");
                throw new GatewayException(mapCode(code), message);
            }
            return json;

        } catch (HttpTimeoutException e) {
            throw new GatewayTimeoutException("PG %s 응답 시간 초과".formatted(action), e);
        } catch (java.io.IOException e) {
            // 네트워크 단절도 결과를 알 수 없는 경우입니다.
            throw new GatewayTimeoutException("PG %s 통신 실패".formatted(action), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new GatewayTimeoutException("PG %s 중단됨".formatted(action), e);
        }
    }

    private Approval toApproval(JsonNode json) {
        String status = json.path("status").asText();
        String methodRaw = json.path("method").asText("");

        PaymentStatus mapped = switch (status) {
            case "DONE" -> PaymentStatus.APPROVED;
            case "WAITING_FOR_DEPOSIT" -> PaymentStatus.WAITING_FOR_DEPOSIT;
            case "CANCELED", "PARTIAL_CANCELED" -> PaymentStatus.CANCELLED;
            default -> throw new GatewayException("NOT_APPROVED",
                    "승인되지 않은 결제입니다 (PG 상태: %s)".formatted(status));
        };

        VirtualAccount va = null;
        JsonNode vaNode = json.path("virtualAccount");
        if (!vaNode.isMissingNode() && !vaNode.isNull()) {
            va = new VirtualAccount(
                    vaNode.path("bankCode").asText(),
                    vaNode.path("accountNumber").asText(),
                    vaNode.path("customerName").asText(),
                    parseInstant(vaNode.path("dueDate").asText(null)));
        }

        return new Approval(
                json.path("paymentKey").asText(),
                mapMethod(methodRaw),
                Money.won(json.path("totalAmount").asLong()),
                mapped,
                parseInstant(json.path("approvedAt").asText(null)),
                json.path("receipt").path("url").asText(null),
                va);
    }

    private PaymentMethod mapMethod(String raw) {
        return switch (raw) {
            case "카드" -> PaymentMethod.CARD;
            case "가상계좌" -> PaymentMethod.VIRTUAL;
            case "계좌이체" -> PaymentMethod.TRANSFER;
            default -> PaymentMethod.EASY;
        };
    }

    /** PG 에러 코드를 우리 도메인 코드로 번역합니다. 프론트가 PG 코드를 알 필요는 없습니다. */
    private String mapCode(String tossCode) {
        return switch (tossCode) {
            case "REJECT_CARD_COMPANY", "EXCEED_MAX_CARD_INSTALLMENT_PLAN",
                 "INVALID_CARD_EXPIRATION", "EXCEED_MAX_DAILY_PAYMENT_COUNT",
                 "EXCEED_MAX_AMOUNT" -> "CARD_DECLINED";
            case "ALREADY_PROCESSED_PAYMENT" -> "ALREADY_PAID";
            case "NOT_FOUND_PAYMENT", "NOT_FOUND_PAYMENT_SESSION" -> "NOT_APPROVED";
            default -> "PAYMENT_FAILED";
        };
    }

    private Instant parseInstant(String value) {
        if (value == null || value.isBlank()) {
            return Instant.now();
        }
        try {
            return OffsetDateTime.parse(value).toInstant();
        } catch (RuntimeException e) {
            return Instant.now();
        }
    }

    private String escape(String value) {
        return value == null ? "" : value.replace("\"", "'").replace("\n", " ");
    }
}

package com.hoonshop.identity.infrastructure.oauth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hoonshop.common.domain.DomainException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * OAuth2 어댑터 공통 HTTP 처리.
 *
 * <p>세 프로바이더의 흐름이 사실상 같습니다 — 인가 코드로 토큰 교환, 토큰으로 프로필 조회.
 * 다른 건 URL과 응답 JSON 구조뿐이라 그 부분만 각 어댑터가 채웁니다.
 *
 * <p>타임아웃을 반드시 겁니다. 소셜 서버가 느려지면 우리 요청 스레드가 그대로 묶입니다.
 */
public abstract class OAuth2HttpSupport {

    private static final Logger log = LoggerFactory.getLogger(OAuth2HttpSupport.class);

    protected final ObjectMapper mapper;
    private final HttpClient http;

    protected OAuth2HttpSupport(ObjectMapper mapper) {
        this.mapper = mapper;
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
    }

    protected JsonNode postForm(String url, Map<String, String> form) {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(8))
                .header("Content-Type", "application/x-www-form-urlencoded;charset=utf-8")
                .POST(HttpRequest.BodyPublishers.ofString(encode(form), StandardCharsets.UTF_8))
                .build();
        return send(request, "토큰 교환");
    }

    protected JsonNode getWithBearer(String url, String accessToken) {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(8))
                .header("Authorization", "Bearer " + accessToken)
                .GET()
                .build();
        return send(request, "프로필 조회");
    }

    private JsonNode send(HttpRequest request, String action) {
        try {
            HttpResponse<String> response =
                    http.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            if (response.statusCode() >= 400) {
                // 응답 본문에 토큰이 섞여 있을 수 있어 그대로 로그에 남기지 않습니다.
                log.warn("소셜 {} 실패 — HTTP {}", action, response.statusCode());
                throw new DomainException.Unauthorized("SOCIAL_AUTH_FAILED",
                        "소셜 로그인에 실패했습니다. 다시 시도해 주세요.");
            }
            return mapper.readTree(response.body());

        } catch (DomainException e) {
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new DomainException.Unauthorized("SOCIAL_AUTH_FAILED", "소셜 로그인이 중단되었습니다.");
        } catch (Exception e) {
            log.error("소셜 {} 통신 오류", action, e);
            throw new DomainException.Unauthorized("SOCIAL_AUTH_FAILED",
                    "소셜 로그인 서버와 통신하지 못했습니다. 잠시 후 다시 시도해 주세요.");
        }
    }

    protected Map<String, String> tokenForm(String grantType, String clientId, String clientSecret,
                                            String code, String redirectUri) {
        Map<String, String> form = new LinkedHashMap<>();
        form.put("grant_type", grantType);
        form.put("client_id", clientId);
        if (clientSecret != null && !clientSecret.isBlank()) {
            form.put("client_secret", clientSecret);
        }
        form.put("code", code);
        form.put("redirect_uri", redirectUri);
        return form;
    }

    private String encode(Map<String, String> form) {
        StringBuilder sb = new StringBuilder();
        form.forEach((k, v) -> {
            if (!sb.isEmpty()) {
                sb.append('&');
            }
            sb.append(URLEncoder.encode(k, StandardCharsets.UTF_8))
                    .append('=')
                    .append(URLEncoder.encode(v == null ? "" : v, StandardCharsets.UTF_8));
        });
        return sb.toString();
    }

    protected String requireAccessToken(JsonNode tokenResponse) {
        String token = tokenResponse.path("access_token").asText(null);
        if (token == null || token.isBlank()) {
            throw new DomainException.Unauthorized("SOCIAL_AUTH_FAILED",
                    "소셜 로그인 토큰을 받지 못했습니다.");
        }
        return token;
    }
}

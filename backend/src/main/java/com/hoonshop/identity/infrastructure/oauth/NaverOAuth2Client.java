package com.hoonshop.identity.infrastructure.oauth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hoonshop.identity.domain.OAuth2Client;
import com.hoonshop.identity.domain.SocialProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 네이버 로그인.
 *
 * <p>네이버의 특이점: 프로필 응답이 {@code response} 객체로 한 겹 감싸여 있고,
 * 최상위 {@code resultcode}가 "00"이 아니면 실패입니다 — HTTP 200이어도 실패일 수 있어
 * 상태 코드만 보면 안 됩니다.
 */
@Component
@ConditionalOnProperty(name = "hoonshop.oauth.naver.client-id")
public class NaverOAuth2Client extends OAuth2HttpSupport implements OAuth2Client {

    private static final String TOKEN_URL = "https://nid.naver.com/oauth2.0/token";
    private static final String PROFILE_URL = "https://openapi.naver.com/v1/nid/me";

    private final String clientId;
    private final String clientSecret;

    public NaverOAuth2Client(ObjectMapper mapper,
                             @Value("${hoonshop.oauth.naver.client-id}") String clientId,
                             @Value("${hoonshop.oauth.naver.client-secret:}") String clientSecret) {
        super(mapper);
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    @Override
    public SocialProvider provider() {
        return SocialProvider.NAVER;
    }

    @Override
    public SocialProfile fetchProfile(String code, String redirectUri) {
        JsonNode token = postForm(TOKEN_URL,
                tokenForm("authorization_code", clientId, clientSecret, code, redirectUri));

        JsonNode body = getWithBearer(PROFILE_URL, requireAccessToken(token));

        if (!"00".equals(body.path("resultcode").asText())) {
            throw new com.hoonshop.common.domain.DomainException.Unauthorized(
                    "SOCIAL_AUTH_FAILED",
                    "네이버 프로필을 가져오지 못했습니다: " + body.path("message").asText(""));
        }

        JsonNode response = body.path("response");
        return new SocialProfile(SocialProvider.NAVER,
                response.path("id").asText(),
                response.path("email").asText(null),
                response.path("nickname").asText(response.path("name").asText(null)));
    }
}

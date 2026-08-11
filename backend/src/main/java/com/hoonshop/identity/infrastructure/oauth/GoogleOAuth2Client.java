package com.hoonshop.identity.infrastructure.oauth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hoonshop.identity.domain.OAuth2Client;
import com.hoonshop.identity.domain.SocialProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 구글 로그인.
 *
 * <p>구글의 특이점: {@code email_verified}가 명시적으로 내려옵니다.
 * false면 이메일이 있어도 없는 것으로 취급합니다 — 미검증 이메일로 기존 계정에 연결하면
 * 계정 탈취 경로가 됩니다.
 */
@Component
@ConditionalOnProperty(name = "hoonshop.oauth.google.client-id")
public class GoogleOAuth2Client extends OAuth2HttpSupport implements OAuth2Client {

    private static final String TOKEN_URL = "https://oauth2.googleapis.com/token";
    private static final String PROFILE_URL = "https://openidconnect.googleapis.com/v1/userinfo";

    private final String clientId;
    private final String clientSecret;

    public GoogleOAuth2Client(ObjectMapper mapper,
                              @Value("${hoonshop.oauth.google.client-id}") String clientId,
                              @Value("${hoonshop.oauth.google.client-secret:}") String clientSecret) {
        super(mapper);
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    @Override
    public SocialProvider provider() {
        return SocialProvider.GOOGLE;
    }

    @Override
    public SocialProfile fetchProfile(String code, String redirectUri) {
        JsonNode token = postForm(TOKEN_URL,
                tokenForm("authorization_code", clientId, clientSecret, code, redirectUri));

        JsonNode me = getWithBearer(PROFILE_URL, requireAccessToken(token));

        String email = me.path("email_verified").asBoolean(false)
                ? me.path("email").asText(null)
                : null;

        return new SocialProfile(SocialProvider.GOOGLE,
                me.path("sub").asText(),
                email,
                me.path("name").asText(null));
    }
}

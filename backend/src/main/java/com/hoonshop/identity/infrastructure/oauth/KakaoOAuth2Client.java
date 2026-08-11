package com.hoonshop.identity.infrastructure.oauth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hoonshop.identity.domain.OAuth2Client;
import com.hoonshop.identity.domain.SocialProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 카카오 로그인.
 *
 * <p>{@code hoonshop.oauth.kakao.client-id}가 설정된 경우에만 빈으로 등록됩니다.
 * 설정이 없으면 아예 뜨지 않고, 서비스는 "설정되지 않음"으로 응답합니다.
 *
 * <p>카카오의 특이점: <b>이메일이 선택 동의 항목</b>이라 없을 수 있습니다.
 * 또 이메일 동의를 했더라도 {@code is_email_valid}/{@code is_email_verified}가 false면
 * 신뢰할 수 없으므로 없는 것으로 취급합니다 — 검증되지 않은 이메일로 기존 계정에 연결하면
 * 계정 탈취 경로가 됩니다.
 */
@Component
// matchIfMissing=false가 기본이지만, 빈 문자열도 "존재"로 판정되므로
// application.yml에 빈 기본값을 두지 않는 것이 이 조건의 전제입니다.
@ConditionalOnProperty(name = "hoonshop.oauth.kakao.client-id")
public class KakaoOAuth2Client extends OAuth2HttpSupport implements OAuth2Client {

    private static final String TOKEN_URL = "https://kauth.kakao.com/oauth/token";
    private static final String PROFILE_URL = "https://kapi.kakao.com/v2/user/me";

    private final String clientId;
    private final String clientSecret;

    public KakaoOAuth2Client(ObjectMapper mapper,
                             @Value("${hoonshop.oauth.kakao.client-id}") String clientId,
                             @Value("${hoonshop.oauth.kakao.client-secret:}") String clientSecret) {
        super(mapper);
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    @Override
    public SocialProvider provider() {
        return SocialProvider.KAKAO;
    }

    @Override
    public SocialProfile fetchProfile(String code, String redirectUri) {
        JsonNode token = postForm(TOKEN_URL,
                tokenForm("authorization_code", clientId, clientSecret, code, redirectUri));

        JsonNode me = getWithBearer(PROFILE_URL, requireAccessToken(token));

        String providerUserId = me.path("id").asText();
        JsonNode account = me.path("kakao_account");

        // 검증된 이메일만 인정합니다.
        String email = null;
        if (account.path("is_email_valid").asBoolean(false)
                && account.path("is_email_verified").asBoolean(false)) {
            email = account.path("email").asText(null);
        }

        String nickname = account.path("profile").path("nickname").asText(null);

        return new SocialProfile(SocialProvider.KAKAO, providerUserId, email, nickname);
    }
}

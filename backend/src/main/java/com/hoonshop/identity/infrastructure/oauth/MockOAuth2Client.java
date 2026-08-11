package com.hoonshop.identity.infrastructure.oauth;

import com.hoonshop.identity.domain.OAuth2Client;
import com.hoonshop.identity.domain.SocialProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 개발용 목 소셜 로그인.
 *
 * <p>{@code hoonshop.oauth.mock=true}(기본값)일 때 세 프로바이더 모두를 목으로 등록합니다.
 * 실제 클라이언트 ID를 설정하면 진짜 어댑터가 함께 뜨는데, 같은 프로바이더가 둘이 되면
 * 목이 아니라 실제 어댑터가 선택되도록 {@code SocialLoginService}가 먼저 등록된 것을 씁니다.
 * 혼동을 피하려면 실연동 시 {@code hoonshop.oauth.mock=false}로 꺼주세요.
 *
 * <p>인가 코드 형식으로 시나리오를 고릅니다.
 * <ul>
 *   <li>{@code code=noemail-홍길동} → 이메일 미제공 (카카오 선택 동의 거부 상황)</li>
 *   <li>{@code code=<이메일>} → 그 이메일로 로그인 (기존 계정 연결 테스트용)</li>
 *   <li>그 외 → {@code <code>@social.example.com}</li>
 * </ul>
 */
@Configuration
@ConditionalOnProperty(name = "hoonshop.oauth.mock", havingValue = "true", matchIfMissing = true)
public class MockOAuth2Client {

    @Bean
    public OAuth2Client mockKakaoClient() {
        return new Mock(SocialProvider.KAKAO);
    }

    @Bean
    public OAuth2Client mockNaverClient() {
        return new Mock(SocialProvider.NAVER);
    }

    @Bean
    public OAuth2Client mockGoogleClient() {
        return new Mock(SocialProvider.GOOGLE);
    }

    private record Mock(SocialProvider provider) implements OAuth2Client {

        @Override
        public boolean isMock() {
            return true;
        }

        @Override
        public SocialProfile fetchProfile(String code, String redirectUri) {
            if (code == null || code.isBlank()) {
                throw new com.hoonshop.common.domain.DomainException.Unauthorized(
                        "SOCIAL_AUTH_FAILED", "인가 코드가 없습니다.");
            }

            // 이메일을 주지 않는 프로바이더 상황 재현
            if (code.startsWith("noemail-")) {
                String nickname = code.substring("noemail-".length());
                return new SocialProfile(provider, provider.code() + "-" + nickname, null,
                        nickname);
            }

            String email = code.contains("@") ? code : code + "@social.example.com";
            String nickname = email.substring(0, email.indexOf('@'));

            // providerUserId는 이메일이 아니라 별도 값이어야 합니다 — 실제 프로바이더와 동일하게.
            return new SocialProfile(provider, provider.code() + "-uid-" + nickname, email,
                    nickname);
        }
    }
}

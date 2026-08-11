package com.hoonshop.identity.domain;

/**
 * 소셜 로그인 연동 포트.
 *
 * <p>프로바이더마다 응답 형식이 제각각입니다 — 카카오는 {@code kakao_account.email},
 * 네이버는 {@code response.email}, 구글은 {@code email}. 그 차이를 어댑터가 흡수하고
 * 도메인에는 {@link SocialProfile} 하나로만 전달합니다.
 *
 * <p>인가 코드(authorization code)를 서버가 교환하는 방식을 씁니다.
 * 프론트가 직접 액세스 토큰을 받아 서버로 넘기면, 토큰이 브라우저에 노출되고
 * 그 토큰이 정말 우리 앱을 위해 발급된 것인지 검증할 수 없습니다.
 */
public interface OAuth2Client {

    SocialProvider provider();

    /**
     * 목 구현인지 여부.
     *
     * <p>같은 프로바이더에 실제 어댑터와 목이 동시에 등록될 수 있어(설정 실수 등)
     * 어느 쪽을 쓸지 결정해야 합니다. 실제 어댑터가 있으면 반드시 그쪽을 씁니다 —
     * 운영에서 목이 선택되면 아무나 로그인되는 상태가 됩니다.
     */
    default boolean isMock() {
        return false;
    }

    /**
     * 인가 코드를 프로필로 교환합니다.
     *
     * @param code        프론트가 소셜 로그인 후 받은 인가 코드
     * @param redirectUri 인가 요청 때 쓴 것과 동일해야 합니다 (프로바이더가 대조합니다)
     */
    SocialProfile fetchProfile(String code, String redirectUri);

    /**
     * 소셜 프로필.
     *
     * @param providerUserId 프로바이더의 불변 사용자 ID — 계정 식별의 유일한 기준
     * @param email          제공되지 않을 수 있습니다 (카카오는 선택 동의 항목)
     * @param nickname       제공되지 않으면 프로바이더명 기반으로 만듭니다
     */
    record SocialProfile(SocialProvider provider, String providerUserId, String email,
                         String nickname) {

        public boolean hasEmail() {
            return email != null && !email.isBlank();
        }
    }
}

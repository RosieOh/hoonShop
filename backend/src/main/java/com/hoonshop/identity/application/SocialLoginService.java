package com.hoonshop.identity.application;

import com.hoonshop.common.domain.DomainException;
import com.hoonshop.identity.domain.*;
import com.hoonshop.identity.infrastructure.JwtTokenProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * 소셜 로그인.
 *
 * <p>세 가지 경우를 다룹니다.
 * <ol>
 *   <li><b>이미 연결된 소셜 계정</b> → 그 사용자로 로그인</li>
 *   <li><b>처음 보는 소셜 계정인데 이메일이 기존 회원과 같음</b> → 기존 계정에 연결</li>
 *   <li><b>완전히 새로움</b> → 회원 가입 + 연결</li>
 * </ol>
 *
 * <p><b>2번이 보안상 가장 민감합니다.</b> 이메일만으로 기존 계정에 붙이는 것은,
 * 프로바이더가 그 이메일의 소유를 검증했을 때만 안전합니다. 검증하지 않는 프로바이더라면
 * 공격자가 남의 이메일로 소셜 계정을 만들어 계정을 탈취할 수 있습니다.
 * 카카오·네이버·구글은 모두 이메일을 검증하므로 자동 연결을 허용했습니다.
 * <b>검증하지 않는 프로바이더를 추가한다면 이 정책을 반드시 다시 검토해야 합니다.</b>
 */
@Service
public class SocialLoginService {

    private static final Logger log = LoggerFactory.getLogger(SocialLoginService.class);

    private final Map<SocialProvider, OAuth2Client> clients;
    private final UserRepository users;
    private final SocialAccountRepository socialAccounts;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;

    public SocialLoginService(List<OAuth2Client> clientList, UserRepository users,
                              SocialAccountRepository socialAccounts,
                              PasswordEncoder passwordEncoder, JwtTokenProvider tokenProvider) {
        // 등록된 어댑터를 프로바이더별로 색인합니다. 설정이 없는 프로바이더는 빈이 없어
        // 여기 들어오지 않고, 호출 시 "지원하지 않음"으로 응답됩니다.
        //
        // 같은 프로바이더에 실제 어댑터와 목이 함께 있으면 실제 쪽을 씁니다.
        // 운영에서 목이 선택되면 인가 코드 검증 없이 아무나 로그인됩니다.
        this.clients = clientList.stream()
                .collect(java.util.stream.Collectors.toMap(OAuth2Client::provider,
                        Function.identity(),
                        (a, b) -> a.isMock() ? b : a));

        clients.forEach((provider, client) -> {
            if (client.isMock()) {
                log.warn("{} 로그인이 목으로 동작합니다. 운영 배포 전 hoonshop.oauth.mock=false 로 바꾸세요.",
                        provider.label());
            }
        });
        this.users = users;
        this.socialAccounts = socialAccounts;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
    }

    @Transactional
    public AuthService.LoginResult login(SocialProvider provider, String code, String redirectUri) {
        OAuth2Client client = clients.get(provider);
        if (client == null) {
            throw new DomainException("PROVIDER_NOT_CONFIGURED",
                    "%s 로그인이 아직 설정되지 않았습니다.".formatted(provider.label()));
        }

        OAuth2Client.SocialProfile profile = client.fetchProfile(code, redirectUri);
        User user = resolveUser(provider, profile);

        return new AuthService.LoginResult(
                AuthService.UserProfile.from(user),
                tokenProvider.createAccessToken(user),
                tokenProvider.createRefreshToken(user),
                tokenProvider.accessTtlSeconds());
    }

    private User resolveUser(SocialProvider provider, OAuth2Client.SocialProfile profile) {
        // 1) 이미 연결된 소셜 계정
        var existingLink = socialAccounts.findByProviderAndProviderUserId(
                provider, profile.providerUserId());

        if (existingLink.isPresent()) {
            SocialAccount account = existingLink.get();
            account.recordLogin();
            socialAccounts.save(account);
            return loadUser(account.userId());
        }

        // 2) 이메일이 기존 회원과 일치 → 연결
        if (profile.hasEmail()) {
            var byEmail = users.findByEmail(Email.of(profile.email()));
            if (byEmail.isPresent()) {
                User user = byEmail.get();
                socialAccounts.save(SocialAccount.link(user.id(), provider,
                        profile.providerUserId(), profile.email()));
                log.info("기존 계정에 소셜 연결 — user={} provider={}", user.id(), provider);
                return user;
            }
        }

        // 3) 신규 가입
        Email email = profile.hasEmail()
                ? Email.of(profile.email())
                : syntheticEmail(provider, profile.providerUserId());

        User created = users.save(User.registerBySocial(email, profile.nickname(), passwordEncoder));
        socialAccounts.save(SocialAccount.link(created.id(), provider, profile.providerUserId(),
                profile.email()));
        log.info("소셜 신규 가입 — user={} provider={}", created.id(), provider);
        return created;
    }

    /**
     * 이메일 미제공 시 쓰는 내부 식별용 주소.
     *
     * <p>실제로 메일이 가지 않는 주소이므로, 주문 알림 같은 기능을 쓰려면 나중에
     * 실제 이메일을 받아야 합니다. {@code .invalid}는 RFC 6761이 "절대 실재하지 않음"으로
     * 예약한 도메인이라, 실수로 외부 발송돼도 남의 주소로 갈 일이 없습니다.
     */
    private Email syntheticEmail(SocialProvider provider, String providerUserId) {
        return Email.of("%s_%s@social.hoonshop.invalid".formatted(provider.code(), providerUserId));
    }

    private User loadUser(Long userId) {
        return users.findById(userId)
                .orElseThrow(() -> new DomainException.Unauthorized("USER_NOT_FOUND",
                        "연결된 회원 정보를 찾을 수 없습니다. 고객센터로 문의해 주세요."));
    }

    /** 프론트가 로그인 버튼을 그릴 때 쓸 수 있도록 활성 프로바이더를 알려줍니다. */
    public List<String> enabledProviders() {
        return clients.keySet().stream().map(SocialProvider::code).sorted().toList();
    }
}

package com.hoonshop.identity.application;

import com.hoonshop.common.domain.DomainException;
import com.hoonshop.identity.domain.*;
import com.hoonshop.identity.infrastructure.JwtTokenProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;

    public AuthService(UserRepository users, PasswordEncoder passwordEncoder,
                       JwtTokenProvider tokenProvider) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
    }

    public LoginResult login(String email, String rawPassword) {
        // 존재하지 않는 이메일도 "인증 실패"로 통일합니다 — 가입 여부가 새어나가지 않도록.
        User user = users.findByEmail(safeEmail(email))
                .orElseThrow(() -> new DomainException.Unauthorized("INVALID_CREDENTIALS",
                        "이메일 또는 비밀번호가 올바르지 않습니다."));

        user.authenticate(rawPassword, passwordEncoder);

        return new LoginResult(UserProfile.from(user),
                tokenProvider.createAccessToken(user),
                tokenProvider.createRefreshToken(user),
                tokenProvider.accessTtlSeconds());
    }

    public RefreshResult refresh(String refreshToken) {
        JwtTokenProvider.Authentication auth = tokenProvider.parseRefreshToken(refreshToken);
        User user = users.findByEmail(Email.of(auth.email()))
                .orElseThrow(() -> new DomainException.Unauthorized("INVALID_TOKEN",
                        "인증 정보가 유효하지 않습니다. 다시 로그인해 주세요."));

        return new RefreshResult(tokenProvider.createAccessToken(user),
                tokenProvider.accessTtlSeconds());
    }

    public UserProfile me(String email) {
        return users.findByEmail(Email.of(email))
                .map(UserProfile::from)
                .orElseThrow(() -> new DomainException.Unauthorized("UNAUTHENTICATED",
                        "인증이 필요합니다."));
    }

    /** 로그인 입력은 신뢰할 수 없으므로, 형식 오류도 인증 실패로 처리합니다. */
    private Email safeEmail(String email) {
        try {
            return Email.of(email);
        } catch (IllegalArgumentException e) {
            throw new DomainException.Unauthorized("INVALID_CREDENTIALS",
                    "이메일 또는 비밀번호가 올바르지 않습니다.");
        }
    }

    public record LoginResult(UserProfile user, String token, String refreshToken, long expiresIn) {
    }

    public record RefreshResult(String token, long expiresIn) {
    }

    public record UserProfile(String id, String email, String name, String role, String grade,
                              int point, Instant joinedAt) {

        static UserProfile from(User user) {
            return new UserProfile("U-%04d".formatted(user.id()), user.email().value(),
                    user.name(), user.role().code(), user.grade(), user.point(), user.joinedAt());
        }
    }
}

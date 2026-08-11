package com.hoonshop.identity.infrastructure;

import com.hoonshop.common.domain.DomainException;
import com.hoonshop.identity.domain.Role;
import com.hoonshop.identity.domain.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

/**
 * JWT 발급·검증.
 *
 * <p>액세스 토큰은 짧게(1시간), 리프레시는 길게(14일) 둡니다. 액세스가 새어나가도
 * 피해 시간이 제한되고, 사용자는 2주에 한 번만 다시 로그인합니다.
 *
 * <p>토큰 타입을 클레임에 박아두는 이유: 리프레시 토큰을 그대로 Authorization 헤더에 넣어
 * API를 호출하는 우회를 막습니다.
 */
@Component
public class JwtTokenProvider {

    private static final String CLAIM_ROLE = "role";
    private static final String CLAIM_TYPE = "typ";
    private static final String TYPE_ACCESS = "access";
    private static final String TYPE_REFRESH = "refresh";

    private final SecretKey key;
    private final Duration accessTtl;
    private final Duration refreshTtl;

    public JwtTokenProvider(
            @Value("${hoonshop.jwt.secret}") String secret,
            @Value("${hoonshop.jwt.access-ttl:PT1H}") Duration accessTtl,
            @Value("${hoonshop.jwt.refresh-ttl:P14D}") Duration refreshTtl) {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalStateException(
                    "hoonshop.jwt.secret은 32바이트 이상이어야 합니다. 현재: " + keyBytes.length);
        }
        this.key = Keys.hmacShaKeyFor(keyBytes);
        this.accessTtl = accessTtl;
        this.refreshTtl = refreshTtl;
    }

    public String createAccessToken(User user) {
        return build(user.email().value(), user.role(), TYPE_ACCESS, accessTtl);
    }

    public String createRefreshToken(User user) {
        return build(user.email().value(), user.role(), TYPE_REFRESH, refreshTtl);
    }

    public long accessTtlSeconds() {
        return accessTtl.toSeconds();
    }

    private String build(String subject, Role role, String type, Duration ttl) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(subject)
                .claim(CLAIM_ROLE, role.name())
                .claim(CLAIM_TYPE, type)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(ttl)))
                .signWith(key)
                .compact();
    }

    public Authentication parseAccessToken(String token) {
        return parse(token, TYPE_ACCESS);
    }

    public Authentication parseRefreshToken(String token) {
        return parse(token, TYPE_REFRESH);
    }

    private Authentication parse(String token, String expectedType) {
        try {
            Claims claims = Jwts.parser().verifyWith(key).build()
                    .parseSignedClaims(token).getPayload();

            if (!expectedType.equals(claims.get(CLAIM_TYPE, String.class))) {
                throw new DomainException.Unauthorized("INVALID_TOKEN_TYPE",
                        "토큰 종류가 올바르지 않습니다.");
            }
            return new Authentication(claims.getSubject(),
                    Role.valueOf(claims.get(CLAIM_ROLE, String.class)));
        } catch (JwtException | IllegalArgumentException e) {
            throw new DomainException.Unauthorized("INVALID_TOKEN",
                    "인증 정보가 유효하지 않습니다. 다시 로그인해 주세요.");
        }
    }

    public record Authentication(String email, Role role) {
    }
}

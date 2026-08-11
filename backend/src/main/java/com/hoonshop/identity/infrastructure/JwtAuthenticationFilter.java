package com.hoonshop.identity.infrastructure;

import com.hoonshop.common.domain.DomainException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Authorization 헤더의 JWT를 SecurityContext로 옮깁니다.
 *
 * <p>토큰이 없거나 깨졌으면 여기서 401을 던지지 않고 그냥 인증 없이 통과시킵니다.
 * 접근 가능 여부 판단은 SecurityConfig의 인가 규칙이 담당해야 하며,
 * 필터가 미리 막으면 공개 API까지 토큰을 요구하게 됩니다.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String HEADER = "Authorization";
    private static final String PREFIX = "Bearer ";

    private final JwtTokenProvider tokenProvider;

    public JwtAuthenticationFilter(JwtTokenProvider tokenProvider) {
        this.tokenProvider = tokenProvider;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        String header = request.getHeader(HEADER);
        if (header != null && header.startsWith(PREFIX)) {
            try {
                JwtTokenProvider.Authentication auth =
                        tokenProvider.parseAccessToken(header.substring(PREFIX.length()));

                var authentication = new UsernamePasswordAuthenticationToken(
                        auth.email(), null,
                        List.of(new SimpleGrantedAuthority(auth.role().authority())));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (DomainException.Unauthorized ignored) {
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }
}

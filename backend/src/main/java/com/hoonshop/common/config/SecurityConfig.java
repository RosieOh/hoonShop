package com.hoonshop.common.config;

import com.hoonshop.identity.infrastructure.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtFilter;
    private final List<String> allowedOrigins;

    public SecurityConfig(JwtAuthenticationFilter jwtFilter,
                          @Value("${hoonshop.cors.allowed-origins}") List<String> allowedOrigins) {
        this.jwtFilter = jwtFilter;
        this.allowedOrigins = allowedOrigins;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                // 세션을 쓰지 않고 토큰만 쓰므로 CSRF 토큰이 필요 없습니다.
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // --- 공개 ---
                        .requestMatchers("/api/auth/login", "/api/auth/refresh").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/products/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/coupons/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/reviews/**", "/api/qna/**").permitAll()
                        // 재고 확인은 로그인 전에도 필요합니다 (장바구니에서 바로 확인)
                        .requestMatchers(HttpMethod.POST, "/api/orders/validate").permitAll()
                        // 소셜 로그인 콜백
                        .requestMatchers("/api/auth/oauth/**").permitAll()
                        // PG 웹훅은 우리 토큰이 없습니다. 대신 HMAC 서명으로 인증합니다.
                        .requestMatchers(HttpMethod.POST, "/api/payments/webhook").permitAll()
                        .requestMatchers("/actuator/health", "/v3/api-docs/**", "/swagger-ui/**",
                                "/swagger-ui.html").permitAll()

                        // --- 관리자 전용 ---
                        // 프론트 가드는 UX용이고, 실제 차단은 여기서 일어납니다.
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")

                        // --- 그 외 인증 필요 ---
                        .anyRequest().authenticated())
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(handler -> handler
                        .authenticationEntryPoint((request, response, ex) -> {
                            response.setStatus(401);
                            response.setContentType("application/json;charset=UTF-8");
                            response.getWriter().write(
                                    "{\"code\":\"UNAUTHENTICATED\",\"message\":\"로그인이 필요합니다.\"}");
                        })
                        .accessDeniedHandler((request, response, ex) -> {
                            response.setStatus(403);
                            response.setContentType("application/json;charset=UTF-8");
                            response.getWriter().write(
                                    "{\"code\":\"FORBIDDEN\",\"message\":\"접근 권한이 없습니다.\"}");
                        }))
                .build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        // 와일드카드(*)를 쓰지 않습니다. 배포 주소를 명시해야 토큰이 아무 사이트로나 실려가지 않습니다.
        config.setAllowedOrigins(allowedOrigins);
        config.setAllowedMethods(List.of("GET", "POST", "PATCH", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }
}

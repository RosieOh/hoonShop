package com.hoonshop.identity.presentation;

import com.hoonshop.common.domain.DomainException;
import com.hoonshop.identity.application.AuthService;
import com.hoonshop.identity.application.SocialLoginService;
import com.hoonshop.identity.domain.SocialProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Auth", description = "인증")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final SocialLoginService socialLoginService;

    public AuthController(AuthService authService, SocialLoginService socialLoginService) {
        this.authService = authService;
        this.socialLoginService = socialLoginService;
    }

    @Operation(summary = "로그인 — 액세스/리프레시 토큰 발급")
    @PostMapping("/login")
    public AuthService.LoginResult login(@RequestBody LoginRequest request) {
        return authService.login(request.email(), request.password());
    }

    @Operation(summary = "액세스 토큰 재발급")
    @PostMapping("/refresh")
    public AuthService.RefreshResult refresh(@RequestBody RefreshRequest request) {
        return authService.refresh(request.refreshToken());
    }

    @Operation(summary = "내 정보")
    @GetMapping("/me")
    public AuthService.UserProfile me(Authentication authentication) {
        if (authentication == null) {
            throw new DomainException.Unauthorized("UNAUTHENTICATED", "인증이 필요합니다.");
        }
        return authService.me(authentication.getName());
    }

    @Operation(summary = "사용 가능한 소셜 로그인 목록",
            description = "설정된 프로바이더만 내려옵니다. 프론트는 이걸로 버튼을 그립니다.")
    @GetMapping("/oauth/providers")
    public ProvidersResponse providers() {
        return new ProvidersResponse(socialLoginService.enabledProviders());
    }

    @Operation(summary = "소셜 로그인",
            description = """
                    프론트가 소셜 인가 화면에서 받은 **인가 코드**를 보내면, 서버가 토큰으로
                    교환하고 프로필을 조회해 우리 JWT를 발급합니다.

                    프론트가 액세스 토큰을 직접 받아 넘기지 않는 이유: 그 토큰이 정말 우리 앱을
                    위해 발급된 것인지 서버가 검증할 수 없고, 브라우저에 노출됩니다.
                    """)
    @PostMapping("/oauth/{provider}")
    public AuthService.LoginResult socialLogin(@PathVariable String provider,
                                               @RequestBody @Valid SocialLoginRequest request) {
        return socialLoginService.login(SocialProvider.fromCode(provider), request.code(),
                request.redirectUri());
    }

    public record SocialLoginRequest(@NotBlank String code, @NotBlank String redirectUri) {
    }

    public record ProvidersResponse(java.util.List<String> providers) {
    }

    public record LoginRequest(@NotBlank String email, @NotBlank String password) {
    }

    public record RefreshRequest(@NotBlank String refreshToken) {
    }
}

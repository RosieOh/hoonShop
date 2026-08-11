package com.hoonshop.identity.presentation;

import com.hoonshop.common.domain.DomainException;
import com.hoonshop.identity.application.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Auth", description = "인증")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
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

    public record LoginRequest(@NotBlank String email, @NotBlank String password) {
    }

    public record RefreshRequest(@NotBlank String refreshToken) {
    }
}

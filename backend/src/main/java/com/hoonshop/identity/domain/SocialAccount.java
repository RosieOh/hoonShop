package com.hoonshop.identity.domain;

import jakarta.persistence.*;

import java.time.Instant;

/**
 * 소셜 계정 연결.
 *
 * <p>사용자 한 명이 카카오·네이버·구글을 모두 연결할 수 있으므로 {@link User}와 1:N입니다.
 *
 * <p><b>식별자는 이메일이 아니라 {@code providerUserId}입니다.</b> 이게 중요합니다.
 * 카카오는 이메일 제공을 거부할 수 있고, 사용자가 소셜 계정의 이메일을 바꿀 수도 있습니다.
 * 이메일로 소셜 계정을 식별하면 그때마다 다른 사람으로 인식되거나, 더 나쁘게는
 * 남의 계정에 붙어버립니다. 프로바이더가 주는 불변 ID를 씁니다.
 */
@Entity
@Table(name = "social_account",
        uniqueConstraints = @UniqueConstraint(name = "uk_social_provider_user",
                columnNames = {"provider", "provider_user_id"}))
public class SocialAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 애그리거트 간 참조는 id로만 합니다 (User 객체를 직접 물지 않습니다). */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SocialProvider provider;

    /** 프로바이더가 발급한 불변 사용자 ID */
    @Column(name = "provider_user_id", nullable = false, length = 100)
    private String providerUserId;

    /** 연결 당시의 이메일. 로그인 판단에는 쓰지 않고 참고용으로만 둡니다. */
    @Column(name = "linked_email", length = 120)
    private String linkedEmail;

    @Column(name = "linked_at", nullable = false)
    private Instant linkedAt;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    protected SocialAccount() {
    }

    private SocialAccount(Long userId, SocialProvider provider, String providerUserId,
                          String linkedEmail) {
        if (providerUserId == null || providerUserId.isBlank()) {
            throw new IllegalArgumentException("소셜 사용자 ID가 없습니다.");
        }
        this.userId = userId;
        this.provider = provider;
        this.providerUserId = providerUserId;
        this.linkedEmail = linkedEmail;
        this.linkedAt = Instant.now();
        this.lastLoginAt = Instant.now();
    }

    public static SocialAccount link(Long userId, SocialProvider provider, String providerUserId,
                                     String linkedEmail) {
        return new SocialAccount(userId, provider, providerUserId, linkedEmail);
    }

    public void recordLogin() {
        this.lastLoginAt = Instant.now();
    }

    public Long userId() {
        return userId;
    }

    public SocialProvider provider() {
        return provider;
    }

    public String providerUserId() {
        return providerUserId;
    }

    public String linkedEmail() {
        return linkedEmail;
    }

    public Instant linkedAt() {
        return linkedAt;
    }
}

package com.hoonshop.identity.domain;

import com.hoonshop.common.domain.AggregateRoot;
import com.hoonshop.common.domain.DomainException;
import jakarta.persistence.*;

import java.time.Instant;

/**
 * 사용자 애그리거트 루트.
 *
 * <p>비밀번호 검증을 서비스가 아니라 여기서 하는 이유: "비밀번호가 맞는가"는 사용자에 대한
 * 규칙이지 애플리케이션의 절차가 아닙니다. 서비스로 빼면 다른 유스케이스가 검증을 건너뛰고
 * 로그인시키는 경로가 생길 수 있습니다.
 */
@Entity
@Table(name = "app_user")
public class User extends AggregateRoot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Embedded
    private Email email;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Column(nullable = false, length = 40)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    @Column(nullable = false, length = 20)
    private String grade;

    @Column(nullable = false)
    private int point;

    @Column(nullable = false)
    private Instant joinedAt;

    protected User() {
    }

    private User(Email email, String passwordHash, String name, Role role, String grade,
                 int point, Instant joinedAt) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.name = name;
        this.role = role;
        this.grade = grade;
        this.point = point;
        this.joinedAt = joinedAt;
    }

    public static User register(Email email, String rawPassword, String name, Role role,
                                PasswordEncoder encoder) {
        if (rawPassword == null || rawPassword.length() < 8) {
            throw new DomainException("WEAK_PASSWORD", "비밀번호는 8자 이상이어야 합니다.");
        }
        if (name == null || name.isBlank()) {
            throw new DomainException("INVALID_NAME", "이름을 입력해 주세요.");
        }
        return new User(email, encoder.encode(rawPassword), name, role,
                role == Role.ADMIN ? "STAFF" : "BASIC", 0, Instant.now());
    }

    /** 시드 데이터 전용 — 등급·포인트·가입일을 지정해 만듭니다. */
    public static User restore(Email email, String rawPassword, String name, Role role,
                               String grade, int point, Instant joinedAt,
                               PasswordEncoder encoder) {
        return new User(email, encoder.encode(rawPassword), name, role, grade, point, joinedAt);
    }

    /**
     * 인증.
     *
     * <p>실패 사유를 "없는 이메일"과 "틀린 비밀번호"로 나누지 않습니다.
     * 나누면 공격자가 가입된 이메일 목록을 수집할 수 있습니다.
     */
    public void authenticate(String rawPassword, PasswordEncoder encoder) {
        if (!encoder.matches(rawPassword, passwordHash)) {
            throw new DomainException.Unauthorized("INVALID_CREDENTIALS",
                    "이메일 또는 비밀번호가 올바르지 않습니다.");
        }
    }

    public boolean isAdmin() {
        return role == Role.ADMIN;
    }

    public void earnPoints(int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("적립 포인트는 음수일 수 없습니다: " + amount);
        }
        this.point += amount;
    }

    public Long id() {
        return id;
    }

    public Email email() {
        return email;
    }

    public String name() {
        return name;
    }

    public Role role() {
        return role;
    }

    public String grade() {
        return grade;
    }

    public int point() {
        return point;
    }

    public Instant joinedAt() {
        return joinedAt;
    }
}

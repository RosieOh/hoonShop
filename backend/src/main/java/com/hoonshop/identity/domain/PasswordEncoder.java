package com.hoonshop.identity.domain;

/**
 * 비밀번호 해싱 포트.
 *
 * <p>도메인이 Spring Security의 {@code PasswordEncoder}를 직접 쓰면 도메인 계층이
 * 프레임워크에 묶입니다. 인터페이스만 도메인에 두고 BCrypt 구현은 infrastructure에 둡니다 —
 * 나중에 Argon2로 바꿔도 도메인은 그대로입니다.
 */
public interface PasswordEncoder {

    String encode(String rawPassword);

    boolean matches(String rawPassword, String encodedPassword);
}

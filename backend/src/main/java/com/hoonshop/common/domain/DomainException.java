package com.hoonshop.common.domain;

/**
 * 도메인 규칙 위반.
 *
 * <p>기술적 오류(NPE, DB 연결 실패)와 구분합니다. 이 예외는 "사용자가 하려던 일이
 * 규칙상 불가능했다"는 뜻이고, 메시지는 그대로 사용자에게 보여줄 수 있어야 합니다.
 * {@code errorCode}는 프론트가 분기 처리할 수 있도록 안정적인 식별자를 제공합니다.
 */
public class DomainException extends RuntimeException {

    private final String errorCode;

    public DomainException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public String errorCode() {
        return errorCode;
    }

    /** 존재하지 않는 리소스 → 404 */
    public static class NotFound extends DomainException {
        public NotFound(String errorCode, String message) {
            super(errorCode, message);
        }
    }

    /** 현재 상태에서 허용되지 않는 전이 → 409 */
    public static class Conflict extends DomainException {
        public Conflict(String errorCode, String message) {
            super(errorCode, message);
        }
    }

    /** 인증/권한 실패 → 401 / 403 */
    public static class Unauthorized extends DomainException {
        public Unauthorized(String errorCode, String message) {
            super(errorCode, message);
        }
    }
}

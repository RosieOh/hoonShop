package com.hoonshop.common.presentation;

import com.hoonshop.common.domain.DomainException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.Map;

/**
 * 예외 → HTTP 응답 번역.
 *
 * <p>도메인은 HTTP를 모르고 상태 코드도 모릅니다. 그 매핑을 여기 한 곳에 모아둡니다.
 *
 * <p>응답 본문은 항상 {@code {code, message}} 형태입니다. 프론트가 {@code code}로 분기하고
 * {@code message}를 그대로 보여줄 수 있어야, 화면마다 에러 문구를 새로 만들지 않습니다.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 일부 도메인 오류는 기본 매핑보다 구체적인 상태 코드가 어울립니다.
     * 결제 거절은 "요청이 잘못됐다"기보다 "지불이 거부됐다"이므로 402입니다.
     */
    private static final Map<String, HttpStatus> STATUS_BY_CODE = Map.of(
            "CARD_DECLINED", HttpStatus.PAYMENT_REQUIRED,
            "OUT_OF_STOCK", HttpStatus.CONFLICT,
            "AMOUNT_MISMATCH", HttpStatus.CONFLICT);

    @ExceptionHandler(DomainException.NotFound.class)
    public ResponseEntity<ApiError> handleNotFound(DomainException.NotFound e) {
        return build(HttpStatus.NOT_FOUND, e.errorCode(), e.getMessage());
    }

    @ExceptionHandler(DomainException.Unauthorized.class)
    public ResponseEntity<ApiError> handleUnauthorized(DomainException.Unauthorized e) {
        return build(HttpStatus.UNAUTHORIZED, e.errorCode(), e.getMessage());
    }

    @ExceptionHandler(DomainException.Conflict.class)
    public ResponseEntity<ApiError> handleConflict(DomainException.Conflict e) {
        return build(STATUS_BY_CODE.getOrDefault(e.errorCode(), HttpStatus.CONFLICT),
                e.errorCode(), e.getMessage());
    }

    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ApiError> handleDomain(DomainException e) {
        return build(HttpStatus.BAD_REQUEST, e.errorCode(), e.getMessage());
    }

    /** 값 객체 생성 실패 등. 메시지가 이미 사용자용이라 그대로 내보냅니다. */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleIllegalArgument(IllegalArgumentException e) {
        return build(HttpStatus.BAD_REQUEST, "INVALID_INPUT", e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> "%s: %s".formatted(error.getField(), error.getDefaultMessage()))
                .orElse("입력값을 확인해 주세요.");
        return build(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", message);
    }

    /**
     * 예상 못 한 오류.
     *
     * <p>내부 메시지를 그대로 내보내지 않습니다 — 스택트레이스나 SQL이 응답에 실리면
     * 스키마 구조가 새어나갑니다. 로그에는 전부 남깁니다.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception e, HttpServletRequest request) {
        log.error("처리되지 않은 예외 — {} {}", request.getMethod(), request.getRequestURI(), e);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
                "일시적인 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.");
    }

    private ResponseEntity<ApiError> build(HttpStatus status, String code, String message) {
        return ResponseEntity.status(status).body(new ApiError(code, message, Instant.now()));
    }

    public record ApiError(String code, String message, Instant timestamp) {
    }
}

package com.woorifisan.platform.global.exception;

import static net.logstash.logback.argument.StructuredArguments.entries;

import com.woorifisan.platform.global.response.ApiResponse;
import com.woorifisan.platform.global.response.ErrorCode;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 필수 파라미터 누락 (쿼리 파라미터, 헤더 등)
    @ExceptionHandler({MissingServletRequestParameterException.class, MissingRequestHeaderException.class})
    public ResponseEntity<ApiResponse<Void>> handleMissingParams(Exception e) {
        log.warn("[GlobalHandler] 필수 파라미터 누락", entries(Map.of(
                "exceptionType", e.getClass().getSimpleName(),
                "errorCode", ErrorCode.MISSING_REQUIRED_PARAM.getCode(),
                "message", e.getMessage()
        )));
        return ResponseEntity
                .badRequest()
                .body(ApiResponse.error(ErrorCode.MISSING_REQUIRED_PARAM));
    }

    // 비즈니스 예외
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException e) {
        ErrorCode errorCode = e.getErrorCode();

        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(ApiResponse.error(errorCode, e.getMessage()));
    }

    // @Valid 유효성 검사 실패
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException e) {
        FieldError fieldError = e.getBindingResult().getFieldError();
        String field   = fieldError != null ? fieldError.getField()          : "UNKNOWN";
        String message = fieldError != null ? fieldError.getDefaultMessage() : "입력값이 올바르지 않습니다.";
        log.warn("[GlobalHandler] 유효성 검사 실패", entries(Map.of(
                "exceptionType", e.getClass().getSimpleName(),
                "errorCode", ErrorCode.INVALID_INPUT.getCode(),
                "field", field,
                "message", message
        )));
        return ResponseEntity
                .badRequest()
                .body(ApiResponse.error(ErrorCode.INVALID_INPUT));
    }

    // 권한 부족 예외 (403 Forbidden - Spring Security)
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDeniedException(AccessDeniedException e) {
        log.warn("[GlobalHandler] 접근 권한 예외", entries(Map.of(
                "exceptionType", e.getClass().getSimpleName(),
                "errorCode", ErrorCode.CHECKING_ADMIN.getCode(),
                "message", e.getMessage()
        )));
        return ResponseEntity
                .status(ErrorCode.CHECKING_ADMIN.getHttpStatus())
                .body(ApiResponse.error(ErrorCode.CHECKING_ADMIN));
    }

    // 인증 실패 예외 (401 Unauthorized - Spring Security)
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthenticationException(AuthenticationException e) {
        log.warn("[GlobalHandler] 인증 실패 예외", entries(Map.of(
                "exceptionType", e.getClass().getSimpleName(),
                "errorCode", ErrorCode.UNAUTHORIZED.getCode(),
                "message", e.getMessage()
        )));
        return ResponseEntity
                .status(ErrorCode.UNAUTHORIZED.getHttpStatus())
                .body(ApiResponse.error(ErrorCode.UNAUTHORIZED));
    }

    // SSE 클라이언트 연결 중단 및 비동기 요청 불가 예외
    // ClientAbortException(IOException의 하위) 및 AsyncRequestNotUsableException 처리
    @ExceptionHandler({java.io.IOException.class, org.springframework.web.context.request.async.AsyncRequestNotUsableException.class})
    public void handleAsyncException(Exception e) {
        log.debug("[GlobalHandler] SSE/Async 클라이언트 연결 중단: {}", e.getMessage());
        // 연결이 이미 끊어졌으므로 아무것도 반환하지 않음 (HttpMessageNotWritableException 방지)
    }

    // 그 외 예외
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
        log.error("[GlobalHandler] 예상치 못한 예외", entries(Map.of(
                "exceptionType", e.getClass().getSimpleName(),
                "errorCode", ErrorCode.INTERNAL_SERVER_ERROR.getCode(),
                "message", String.valueOf(e.getMessage())
        )), e);
        return ResponseEntity
                .internalServerError()
                .body(ApiResponse.error(ErrorCode.INTERNAL_SERVER_ERROR));
    }
}

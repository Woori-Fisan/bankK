package com.woorifisan.monitoring.global.exception;

import com.woorifisan.monitoring.global.response.ApiResponse;
import com.woorifisan.monitoring.global.response.ErrorCode;
import com.woorifisan.monitoring.global.response.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 비즈니스 예외 처리
     */
    @ExceptionHandler(BusinessException.class)
    protected ResponseEntity<ApiResponse<?>> handleBusinessException(BusinessException e) {
        log.error("BusinessException: {}", e.getMessage());
        ErrorCode errorCode = e.getErrorCode();
        return new ResponseEntity<>(
                ApiResponse.error(ErrorResponse.of(errorCode, e.getMessage())),
                errorCode.getHttpStatus()
        );
    }

    /**
     * Validation 예외 처리 (@RequestBody + @Valid)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    protected ResponseEntity<ApiResponse<?>> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        log.error("MethodArgumentNotValidException: {}", e.getMessage());
        String errorMessage = e.getBindingResult().getAllErrors().get(0).getDefaultMessage();
        return new ResponseEntity<>(
                ApiResponse.error(ErrorCode.INVALID_INPUT.getCode(), errorMessage),
                ErrorCode.INVALID_INPUT.getHttpStatus()
        );
    }

    /**
     * Validation 예외 처리 (@ModelAttribute + @Valid)
     */
    @ExceptionHandler(BindException.class)
    protected ResponseEntity<ApiResponse<?>> handleBindException(BindException e) {
        log.error("BindException: {}", e.getMessage());
        String errorMessage = e.getBindingResult().getAllErrors().get(0).getDefaultMessage();
        return new ResponseEntity<>(
                ApiResponse.error(ErrorCode.INVALID_INPUT.getCode(), errorMessage),
                ErrorCode.INVALID_INPUT.getHttpStatus()
        );
    }

    /**
     * 그 외 모든 예외 처리
     */
    @ExceptionHandler(Exception.class)
    protected ResponseEntity<ApiResponse<?>> handleException(Exception e) {
        log.error("Exception: ", e);
        return new ResponseEntity<>(
                ApiResponse.error(ErrorResponse.of(ErrorCode.INTERNAL_SERVER_ERROR)),
                ErrorCode.INTERNAL_SERVER_ERROR.getHttpStatus()
        );
    }
}

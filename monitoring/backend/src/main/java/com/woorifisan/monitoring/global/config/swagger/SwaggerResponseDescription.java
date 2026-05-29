package com.woorifisan.monitoring.global.config.swagger;

import com.woorifisan.monitoring.global.response.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

/**
 * Swagger에 표시할 에러 응답들을 그룹화한 Enum
 */
@Getter
@RequiredArgsConstructor
public enum SwaggerResponseDescription {

    USER_LOGIN(List.of(
            ErrorCode.INVALID_INPUT,
            ErrorCode.LOGIN_FAILED
    )),

    USER_REGISTER(List.of(
            ErrorCode.INVALID_INPUT,
            ErrorCode.DUPLICATE_LOGIN_ID
    )),

    AUTH_REFRESH(List.of(
            ErrorCode.INVALID_TOKEN,
            ErrorCode.EXPIRED_TOKEN,
            ErrorCode.TOKEN_REUSE_DETECTED,
            ErrorCode.ALREADY_LOGGED_OUT
    )),

    LOG_INQUIRY(List.of(
            ErrorCode.INVALID_INPUT,
            ErrorCode.LOG_FETCH_ERROR,
            ErrorCode.INQUIRY_INVALID_DATE_RANGE
    )),

    LOG_SUMMARY(List.of(
            ErrorCode.INVALID_INPUT,
            ErrorCode.INQUIRY_INVALID_DATE_RANGE,
            ErrorCode.NOT_EXIST_LOG
    ));

    private final List<ErrorCode> errorCodes;
}

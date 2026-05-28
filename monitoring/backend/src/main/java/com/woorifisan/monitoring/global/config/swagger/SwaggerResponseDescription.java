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

    LOG_INQUIRY(List.of(
            ErrorCode.INVALID_INPUT,
            ErrorCode.LOG_FETCH_ERROR,
            ErrorCode.INQUIRY_INVALID_DATE_RANGE
    ));

    private final List<ErrorCode> errorCodes;
}

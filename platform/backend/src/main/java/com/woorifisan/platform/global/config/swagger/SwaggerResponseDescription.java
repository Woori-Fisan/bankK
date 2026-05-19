package com.woorifisan.platform.global.config.swagger;

import com.woorifisan.platform.global.response.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

/**
 * Swagger에 표시할 에러 응답들을 그룹화한 Enum
 */
@Getter
@RequiredArgsConstructor
public enum SwaggerResponseDescription {

    AUTH_LOGIN(List.of(
            ErrorCode.INVALID_INPUT,
            ErrorCode.INVALID_CREDENTIALS,
            ErrorCode.ACCOUNT_LOCKED,
            ErrorCode.ACCOUNT_DELETED
    )),

    AUTH_REFRESH(List.of(
            ErrorCode.INVALID_TOKEN,
            ErrorCode.EXPIRED_TOKEN,
            ErrorCode.USER_NOT_FOUND
    )),

    BANK_TRANSFER(List.of(
            ErrorCode.BANK_NOT_FOUND,
            ErrorCode.BANK_API_ERROR,
            ErrorCode.DUPLICATE_REQUEST
    )),

    USER_MANAGEMENT(List.of(
            ErrorCode.USER_NOT_FOUND,
            ErrorCode.USER_ALREADY_EXISTS
    ));

    private final List<ErrorCode> errorCodes;
}

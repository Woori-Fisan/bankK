package com.woorifisan.bank.global.swagger;

import com.woorifisan.bank.global.response.ErrorCode;
import java.util.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Swagger에 표시할 에러 응답들을 그룹화한 Enum
 */
@Getter
@RequiredArgsConstructor
public enum SwaggerResponseDescription {

    USER_MANAGEMENT(List.of(
            ErrorCode.USER_NOT_FOUND,
            ErrorCode.INVALID_INPUT
    )),

    ACCOUNT_INQUIRY(List.of(
            ErrorCode.ACCOUNT_NOT_FOUND,
            ErrorCode.INVALID_INPUT,
            ErrorCode.INTERNAL_SERVER_ERROR
    ));

    private final List<ErrorCode> errorCodes;
}

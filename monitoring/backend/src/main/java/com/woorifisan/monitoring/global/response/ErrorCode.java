package com.woorifisan.monitoring.global.response;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // Common
    INVALID_TOKEN("AUTH_002", "유효하지 않은 토큰입니다.", HttpStatus.UNAUTHORIZED),
    EXPIRED_TOKEN("AUTH_003", "만료된 토큰입니다.", HttpStatus.UNAUTHORIZED),

    INTERNAL_SERVER_ERROR("ERR_002", "서버 내부 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),

    // Monitoring
    INVALID_INPUT("MON_001", "잘못된 입력값입니다.", HttpStatus.BAD_REQUEST),
    INQUIRY_INVALID_DATE_RANGE("MON_002", "조회 기간을 확인해주세요.", HttpStatus.BAD_REQUEST),
    TRANSFER_PARAMETER_FAULT("MON_003", "필수 항목이 누락되었습니다.", HttpStatus.BAD_REQUEST),
    LOG_FETCH_ERROR("MON_004", "로그 데이터를 가져오는 중 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),

    // user
    LOGIN_FAILED("AUTH_001", "아이디 또는 비밀번호가 일치하지 않습니다.", HttpStatus.UNAUTHORIZED),
    DUPLICATE_LOGIN_ID("AUTH_004", "이미 사용 중인 아이디입니다.", HttpStatus.CONFLICT),
    // USER_NOT_FOUND("AUTH_005", "사용자를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    ;

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;

}

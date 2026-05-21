package com.woorifisan.bank.global.response;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // 공통
    INVALID_INPUT("ERR_001", "잘못된 입력값입니다.", HttpStatus.BAD_REQUEST),
    INTERNAL_SERVER_ERROR("ERR_002", "서버 내부 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
    MISSING_REQUIRED_PARAM("ERR_003", "필수 항목이 누락되었습니다.", HttpStatus.BAD_REQUEST),

    // 인증
    UNAUTHORIZED("AUTH_001", "인증이 필요합니다.", HttpStatus.UNAUTHORIZED),
    FORBIDDEN("AUTH_002", "접근 권한이 없습니다.", HttpStatus.FORBIDDEN),

    // 사용자
    USER_NOT_FOUND("USER_001", "존재하지 않는 사용자입니다.", HttpStatus.NOT_FOUND),

    // 계좌
    ACCOUNT_NOT_FOUND("ACC_001", "계좌를 찾을 수 없거나 고객 정보가 일치하지 않습니다.", HttpStatus.NOT_FOUND),

    // 은행
    BANK_NOT_FOUND("BANK_001", "존재하지 않는 은행입니다.", HttpStatus.NOT_FOUND),
    BANK_PW_ERROR("BANK_002", "계좌 비밀번호가 틀렸습니다.", HttpStatus.FORBIDDEN),
    // 멱등성
    DUPLICATE_REQUEST("IDEM_001", "중복 요청입니다.", HttpStatus.CONFLICT);
    // 이체

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;
}
package com.woorifisan.platform.global.response;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // 공통
    INVALID_INPUT("ERR_001", "잘못된 입력값입니다.", HttpStatus.BAD_REQUEST),
    INTERNAL_SERVER_ERROR("ERR_002", "서버 내부 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),

    // 인증
    UNAUTHORIZED("AUTH_001", "인증이 필요합니다.", HttpStatus.UNAUTHORIZED),
    INVALID_TOKEN("AUTH_002", "유효하지 않은 토큰입니다.", HttpStatus.UNAUTHORIZED),
    EXPIRED_TOKEN("AUTH_003", "만료된 토큰입니다.", HttpStatus.UNAUTHORIZED),
    INVALID_CREDENTIALS("AUTH_004", "아이디 또는 비밀번호가 올바르지 않습니다.", HttpStatus.UNAUTHORIZED),
    ACCOUNT_LOCKED("AUTH_005", "계정이 잠겼습니다. 관리자에게 문의하세요.", HttpStatus.FORBIDDEN),
    ACCOUNT_DELETED("AUTH_006", "삭제된 계정입니다.", HttpStatus.FORBIDDEN),
    ALREADY_LOGGED_OUT("AUTH_008", "이미 로그아웃된 계정입니다.", HttpStatus.UNAUTHORIZED),
    TOKEN_REUSE_DETECTED("AUTH_009", "토큰 재사용이 감지되었습니다.", HttpStatus.UNAUTHORIZED),

    // 권한
    FORBIDDEN("AUTH_007", "접근 권한이 없습니다.", HttpStatus.FORBIDDEN),

    // 사용자
    USER_NOT_FOUND("USER_001", "존재하지 않는 사용자입니다.", HttpStatus.NOT_FOUND),
    USER_ALREADY_EXISTS("USER_002", "이미 존재하는 사용자입니다.", HttpStatus.CONFLICT),

    // 은행
    BANK_NOT_FOUND("BANK_001", "존재하지 않는 은행입니다.", HttpStatus.NOT_FOUND),
    BANK_API_ERROR("BANK_002", "은행 API 호출 중 오류가 발생했습니다.", HttpStatus.BAD_GATEWAY),

    // 멱등성
    DUPLICATE_REQUEST("IDEM_001", "중복 요청입니다.", HttpStatus.CONFLICT);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;
}
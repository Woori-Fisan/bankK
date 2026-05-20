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
    BANK_PW_ERROR("BANK_003", "계좌 비밀번호가 틀렸습니다.", HttpStatus.FORBIDDEN),

    //조회
    INQUIRY_ACCOUNT_NOTFOUND("INQUIRY_001", "사용자의 정보를 찾을 수 없습니다.", HttpStatus.BAD_REQUEST),
    INQUIRY_INVALID_DATE_RANGE("INQUIRY_002", "조회 기간을 확인해주세요.", HttpStatus.BAD_REQUEST),


    // 멱등성
    DUPLICATE_REQUEST("IDEM_001", "중복 요청입니다.", HttpStatus.CONFLICT),

    // 이체
    TRANSFER_PARAMETER_FAULT("TRANSFER_001", "필수 항목이 누락되었습니다.", HttpStatus.BAD_REQUEST),
    TRANSFER_DEPOSIT_ACCOUNT_FAULT("TRANSFER_002", "입금 계좌가 유효하지 않습니다.", HttpStatus.BAD_REQUEST),
    TRANSFER_DEPOSIT_ACCOUNT_STATUS_FAULT("TRANSFER_003", "입금 계좌가 정지되었습니다.", HttpStatus.BAD_REQUEST),

    TRANSFER_WITHDRAW_ACCOUNT_FAULT("TRANSFER_004", "출금 계좌가 유효하지 않습니다.", HttpStatus.BAD_REQUEST),
    TRANSFER_WITHDRAW_ACCOUNT_STATUS_FAULT("TRANSFER_005", "출금 계좌가 정지되었습니다.", HttpStatus.BAD_REQUEST),
    TRANSFER_AMOUNT_FAULT("TRANSFER_006", "이체 금액이 유효하지 않습니다.", HttpStatus.BAD_REQUEST),
    TRANSFER_WITHDRAW_AMOUNT_FAULT("TRANSFER_007", "이체 잔액이 부족합니다.", HttpStatus.BAD_REQUEST),
    TRANSFER_INSERT_FAULT("TRANSFER_008", "거래 기록 저장에 실패했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
    // 대출 중개
    LOAN_BANK_ROUTING_ERROR("LOAN_001", "은행 대출 API 호출 중 오류가 발생했습니다.", HttpStatus.BAD_GATEWAY),
    LOAN_EVALUATION_NOT_FOUND("LOAN_002", "존재하지 않는 심사 건입니다.", HttpStatus.NOT_FOUND),
    LOAN_PRODUCT_NOT_FOUND("LOAN_003", "존재하지 않는 대출 상품입니다.", HttpStatus.NOT_FOUND),
    LOAN_ALREADY_EXECUTED("LOAN_004", "이미 실행된 대출 건입니다.", HttpStatus.CONFLICT),
    LOAN_EVALUATION_REJECTED("LOAN_005", "대출 심사가 거절되었습니다.", HttpStatus.UNPROCESSABLE_ENTITY),
    LOAN_TERMS_NOT_AGREED("LOAN_006", "필수 약관에 동의하지 않았습니다.", HttpStatus.BAD_REQUEST);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;
}
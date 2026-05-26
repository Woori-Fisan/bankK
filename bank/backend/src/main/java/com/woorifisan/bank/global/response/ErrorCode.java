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

    // 은행
    BANK_NOT_FOUND("BANK_001", "존재하지 않는 은행입니다.", HttpStatus.NOT_FOUND),
    BANK_PW_ERROR("BANK_002", "계좌 비밀번호가 틀렸습니다.", HttpStatus.FORBIDDEN),
    // 멱등성
    DUPLICATE_REQUEST("IDEM_001", "중복 요청입니다.", HttpStatus.CONFLICT),

    // 대출
    LOAN_PRODUCT_NOT_FOUND("LOAN_001", "존재하지 않는 대출 상품입니다.", HttpStatus.NOT_FOUND),
    LOAN_NOT_FOUND("LOAN_002", "존재하지 않는 대출 심사 건입니다.", HttpStatus.NOT_FOUND),
    LOAN_ALREADY_EXECUTED("LOAN_003", "이미 실행된 대출입니다.", HttpStatus.CONFLICT),
    LOAN_INVALID_STATUS("LOAN_004", "대출 심사 상태가 유효하지 않습니다.", HttpStatus.BAD_REQUEST),
    LOAN_ACCOUNT_NOT_FOUND("LOAN_005", "입금 계좌를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    LOAN_ACCOUNT_ABNORMAL("LOAN_006", "입금 계좌가 정상 상태가 아닙니다.", HttpStatus.BAD_REQUEST),
    LOAN_ACCOUNT_PASSWORD_MISMATCH("LOAN_007", "계좌 비밀번호가 일치하지 않습니다.", HttpStatus.FORBIDDEN),
    LOAN_CUSTOMER_NOT_FOUND("LOAN_008", "고객 정보를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    LOAN_INCOMPLETE_SALE_PREVENTION("LOAN_009", "동일 상품은 60일 이내 재실행이 불가합니다.", HttpStatus.CONFLICT),
    LOAN_EXCEED_APPROVED_LIMIT("LOAN_010", "실행 금액이 승인 한도를 초과했습니다.", HttpStatus.BAD_REQUEST),
    LOAN_CUSTOMER_IDENTITY_MISMATCH("LOAN_011", "고객 본인 확인에 실패했습니다.", HttpStatus.FORBIDDEN),

    //조회
    INQUIRY_ACCOUNT_NOTFOUND("INQUIRY_001", "사용자의 정보를 찾을 수 없습니다.", HttpStatus.BAD_REQUEST),
    INQUIRY_INVALID_DATE_RANGE("INQUIRY_002", "조회 기간을 확인해주세요.", HttpStatus.BAD_REQUEST),
    ;

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;
}
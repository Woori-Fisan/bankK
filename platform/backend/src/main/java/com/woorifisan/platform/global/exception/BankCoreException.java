package com.woorifisan.platform.global.exception;

import com.woorifisan.platform.global.response.ErrorCode;
import lombok.Getter;

/**
 * 은행 코어 서버가 반환한 에러 응답을 담는 전용 예외.
 *
 * <p>{@link BusinessException}을 상속하므로 {@link GlobalExceptionHandler}의
 * 기존 처리 흐름을 그대로 따르면서, 은행 원본 에러 정보를 추가로 보존한다.</p>
 *
 * @see com.woorifisan.platform.domain.bank.external.client.BankExternalClient
 */
@Getter
public class BankCoreException extends BusinessException {

    /** 은행 코어 서버가 응답한 원본 에러 코드 (예: {@code ACC_006}) */
    private final String bankErrorCode;

    /** 은행 코어 서버가 응답한 원본 에러 메시지 */
    private final String bankErrorMessage;

    /** 은행 코어 서버의 HTTP 응답 상태 코드 (예: 400, 404, 500) */
    private final int bankHttpStatus;

    public BankCoreException(ErrorCode errorCode, String bankErrorCode, String bankErrorMessage, int bankHttpStatus) {
        super(errorCode);
        this.bankErrorCode    = bankErrorCode;
        this.bankErrorMessage = bankErrorMessage;
        this.bankHttpStatus   = bankHttpStatus;
    }
}
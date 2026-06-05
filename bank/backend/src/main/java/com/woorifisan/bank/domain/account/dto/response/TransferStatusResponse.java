package com.woorifisan.bank.domain.account.dto.response;

import lombok.Builder;
import lombok.Getter;

/**
 * 거래 상태 조회 응답 DTO
 */
@Getter
@Builder
public class TransferStatusResponse {
    private String txId;
    private String status;  // SUCCESS, FAILED, PENDING 등
    private String message;
}

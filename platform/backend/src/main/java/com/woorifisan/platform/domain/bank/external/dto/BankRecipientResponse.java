package com.woorifisan.platform.domain.bank.external.dto;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 은행 코어 시스템으로부터 수신한 수취인 조회 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class BankRecipientResponse {

    private String depositorName;
    private String depositBankName;
    private String depositAccountNo;
    private String accountStatus;

}

package com.woorifisan.platform.domain.bank.external.dto;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 은행 코어 시스템으로 전송할 수취인 조회 요청 DTO
 */
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class BankRecipientRequest {

    private String depositBankCode;
    private String depositAccountNo;

    public static BankRecipientRequest of(String depositBankCode, String depositAccountNo) {
        return BankRecipientRequest.builder()
                .depositBankCode(depositBankCode)
                .depositAccountNo(depositAccountNo)
                .build();
    }
}

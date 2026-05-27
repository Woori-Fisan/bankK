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
    private String reqPayload;      // 암호화된 전체 페이로드 (JWE)
    private String bankKeyId;       // 은행 측 개인키 식별용 ID

    public static BankRecipientRequest of(String depositBankCode, String reqPayload, String bankKeyId) {
        return BankRecipientRequest.builder()
                .depositBankCode(depositBankCode)
                .reqPayload(reqPayload)
                .bankKeyId(bankKeyId)
                .build();
    }
}

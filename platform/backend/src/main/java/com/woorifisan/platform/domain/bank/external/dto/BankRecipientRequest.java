package com.woorifisan.platform.domain.bank.external.dto;

import com.woorifisan.platform.global.security.dto.SecureRequest;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * 은행 코어 시스템으로 전송할 수취인 조회 요청 DTO
 */
@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class BankRecipientRequest extends SecureRequest {

    private String depositBankCode;

    public static BankRecipientRequest of(String depositBankCode, String reqPayload, String bankKeyId) {
        return BankRecipientRequest.builder()
                .depositBankCode(depositBankCode)
                .reqPayload(reqPayload)
                .bankKeyId(bankKeyId)
                .build();
    }
}

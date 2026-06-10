package com.woorifisan.platform.domain.bank.external.dto;

import com.woorifisan.platform.global.security.dto.SecureRequest;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * 은행 코어 시스템으로 전송할 잔액 조회 요청 DTO
 */
@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BankBalanceInquiryRequest extends SecureRequest {

    public static BankBalanceInquiryRequest of(String reqPayload, String bankKeyId) {
        return BankBalanceInquiryRequest.builder()
                .reqPayload(reqPayload)
                .bankKeyId(bankKeyId)
                .build();
    }
}

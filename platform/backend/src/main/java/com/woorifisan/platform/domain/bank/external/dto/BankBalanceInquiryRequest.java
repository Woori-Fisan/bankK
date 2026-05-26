package com.woorifisan.platform.domain.bank.external.dto;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 은행 코어 시스템으로 전송할 잔액 조회 요청 DTO
 */
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class BankBalanceInquiryRequest {

    private String encryptedKey;
    private String jwsSignature;
    private String accountNo;
    private String customerRrnPrefix;

    public static BankBalanceInquiryRequest of(String encryptedKey, String jwsSignature, String accountNo, String customerRrnPrefix) {
        return BankBalanceInquiryRequest.builder()
                .encryptedKey(encryptedKey)
                .jwsSignature(jwsSignature)
                .accountNo(accountNo)
                .customerRrnPrefix(customerRrnPrefix)
                .build();
    }
}

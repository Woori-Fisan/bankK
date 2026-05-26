package com.woorifisan.platform.domain.bank.external.dto;

import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 은행 코어 시스템으로 전송할 출금 요청 DTO
 */
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class BankWithdrawalRequest {

    private String encryptedKey;
    private String jwsSignature;
    private String withdrawalAccountNo;
    private String withdrawalPassword;
    private String customerRrnPrefix;
    private BigDecimal amount;

    public static BankWithdrawalRequest of(String encryptedKey, String jwsSignature, String withdrawalAccountNo, String withdrawalPassword, String customerRrnPrefix, BigDecimal amount) {
        return BankWithdrawalRequest.builder()
                .encryptedKey(encryptedKey)
                .jwsSignature(jwsSignature)
                .withdrawalAccountNo(withdrawalAccountNo)
                .withdrawalPassword(withdrawalPassword)
                .customerRrnPrefix(customerRrnPrefix)
                .amount(amount)
                .build();
    }
}

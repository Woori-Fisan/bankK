package com.woorifisan.platform.domain.bank.external.dto;

import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 은행 코어 시스템으로 전송할 타행 이체 출금 요청 DTO
 */
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class BankTransferWithdrawRequest {

    private String encryptedKey;
    private String jwsSignature;
    private String withdrawalAccountNo;
    private String withdrawalPassword;
    private String customerRrnPrefix;
    private String depositBankCode;
    private String depositAccountNo;
    private BigDecimal amount;

    public static BankTransferWithdrawRequest of(String encryptedKey, String jwsSignature, String withdrawalAccountNo, String withdrawalPassword, String customerRrnPrefix, String depositBankCode, String depositAccountNo, BigDecimal amount) {
        return BankTransferWithdrawRequest.builder()
                .encryptedKey(encryptedKey)
                .jwsSignature(jwsSignature)
                .withdrawalAccountNo(withdrawalAccountNo)
                .withdrawalPassword(withdrawalPassword)
                .customerRrnPrefix(customerRrnPrefix)
                .depositBankCode(depositBankCode)
                .depositAccountNo(depositAccountNo)
                .amount(amount)
                .build();
    }
}

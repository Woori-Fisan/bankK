package com.woorifisan.platform.domain.bank.external.dto;

import com.woorifisan.platform.global.security.dto.SecureRequest;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * 은행 코어 시스템으로 전송할 타행 이체 출금 요청 DTO
 */
@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class BankTransferWithdrawRequest extends SecureRequest {

    private String withdrawalBankCode;
    private String depositBankCode;
    private BigDecimal amount;

    public static BankTransferWithdrawRequest of(String reqPayload, String bankKeyId, String withdrawalBankCode, String depositBankCode, BigDecimal amount) {
        return BankTransferWithdrawRequest.builder()
                .reqPayload(reqPayload)
                .bankKeyId(bankKeyId)
                .withdrawalBankCode(withdrawalBankCode)
                .depositBankCode(depositBankCode)
                .amount(amount)
                .build();
    }
}

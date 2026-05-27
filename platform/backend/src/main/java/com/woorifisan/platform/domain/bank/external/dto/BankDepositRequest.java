package com.woorifisan.platform.domain.bank.external.dto;

import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 은행 코어 시스템으로 전송할 입금 요청 DTO
 */
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class BankDepositRequest {

    private String depositAccountNo;
    private BigDecimal amount;
    private String withdrawalBankCode;
    private String withdrawalAccountNo;

    public static BankDepositRequest of(String depositAccountNo, BigDecimal amount, String withdrawalBankCode, String withdrawalAccountNo) {
        return BankDepositRequest.builder()
                .depositAccountNo(depositAccountNo)
                .amount(amount)
                .withdrawalBankCode(withdrawalBankCode)
                .withdrawalAccountNo(withdrawalAccountNo)
                .build();
    }
}

package com.woorifisan.platform.domain.bank.external.dto;

import com.woorifisan.platform.global.security.dto.SecureRequest;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * 은행 코어 시스템으로 전송할 입금 요청 DTO
 */
@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class BankDepositRequest extends SecureRequest {

    private BigDecimal amount;
    private String withdrawalBankCode;

    public static BankDepositRequest of(String reqPayload, String bankKeyId, BigDecimal amount, String withdrawalBankCode) {
        return BankDepositRequest.builder()
                .reqPayload(reqPayload)
                .bankKeyId(bankKeyId)
                .amount(amount)
                .withdrawalBankCode(withdrawalBankCode)
                .build();
    }
}

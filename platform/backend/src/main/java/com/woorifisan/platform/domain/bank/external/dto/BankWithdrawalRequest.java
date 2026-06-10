package com.woorifisan.platform.domain.bank.external.dto;

import com.woorifisan.platform.global.security.dto.SecureRequest;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * 은행 코어 시스템으로 전송할 출금 요청 DTO (E2EE 적용)
 */
@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class BankWithdrawalRequest extends SecureRequest {

    private BigDecimal amount;

    public static BankWithdrawalRequest of(String reqPayload, String bankKeyId, BigDecimal amount) {
        return BankWithdrawalRequest.builder()
                .reqPayload(reqPayload)
                .bankKeyId(bankKeyId)
                .amount(amount)
                .build();
    }
}

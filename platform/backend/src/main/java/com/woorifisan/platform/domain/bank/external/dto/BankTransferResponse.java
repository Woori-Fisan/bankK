package com.woorifisan.platform.domain.bank.external.dto;

import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * 은행 코어 시스템으로부터 수신한 이체 응답 DTO
 */
@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class BankTransferResponse {

    private String transactionId;
    private String transactionDate;
    private BigDecimal balanceAfter;
    private String resPayload;

}

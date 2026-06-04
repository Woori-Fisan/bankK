package com.woorifisan.platform.domain.bank.external.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.woorifisan.platform.global.security.dto.SecureRequest;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Platform → Bank 대출 심사 요청 data 파트 DTO (E2EE 적용)
 */
@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class BankLoanEvaluateRequest extends SecureRequest {

    private String requestKey;
    private String depositBankCode;
    private BigDecimal requestedAmount;
    private Integer requestedPeriod;

    @JsonProperty("isCreditInfoAgreed")
    private boolean creditInfoAgreed;

    @JsonProperty("isProductTermsAgreed")
    private boolean productTermsAgreed;

    @JsonProperty("isDocumentCollected")
    private boolean documentCollected;
}

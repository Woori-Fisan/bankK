package com.woorifisan.platform.domain.bank.external.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

// Platform → Bank 대출 심사 요청 data 파트 DTO
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class BankLoanEvaluateRequest {

    private String requestKey;
    private String customerName;
    private String customerRrnPrefix;
    private String depositBankCode;
    private String depositAccountNo;
    private BigDecimal requestedAmount;
    private Integer requestedPeriod;

    // boolean 필드는 Jackson이 getter 이름에서 'is'를 제거하므로 @JsonProperty로 키 이름 고정
    @JsonProperty("isCreditInfoAgreed")
    private boolean creditInfoAgreed;

    @JsonProperty("isProductTermsAgreed")
    private boolean productTermsAgreed;

    @JsonProperty("isDocumentCollected")
    private boolean documentCollected;
}

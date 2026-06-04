package com.woorifisan.bank.domain.loan.dto.request;

import com.woorifisan.bank.global.security.dto.SecureRequest;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * 암호화된 대출 심사 요청 DTO
 * 민감 정보는 reqPayload 내에 암호화되어 전달됩니다.
 */
@Getter
@SuperBuilder
@NoArgsConstructor
public class LoanEvaluateRequest extends SecureRequest {

    // Bank 자신의 bank.code 와 일치해야됨
    @NotBlank
    private String depositBankCode;

    // 최소 100,000원 이상 신청 가능
    @NotNull
    @DecimalMin("100000")
    private BigDecimal requestedAmount; // 고객 신청 금액

    // 최소 1개월 이상
    @NotNull
    @Min(1)
    private Integer requestedPeriod;    // 신청 상환 기간 (개월)

    @NotNull
    private Boolean isCreditInfoAgreed;    // 개인 신용 정보 이용 동의

    @NotNull
    private Boolean isProductTermsAgreed;  // 상품 약관 동의

    @NotNull
    private Boolean isDocumentCollected;   // 서류 징구 완료

    @NotBlank
    private String requestKey;
}

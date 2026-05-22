package com.woorifisan.bank.domain.loan.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class LoanEvaluateRequest {

    @NotBlank
    private String customerName;

    @NotBlank
    private String customerRrnPrefix;

    @NotBlank
    private String depositAccountNo;

    @NotNull
    @DecimalMin("100000")
    private BigDecimal requestedAmount;

    @NotNull
    @Min(1)
    private Integer requestedPeriod;

    @NotNull
    private Boolean isCreditInfoAgreed;

    @NotNull
    private Boolean isProductTermsAgreed;

    @NotNull
    private Boolean isDocumentCollected;
}

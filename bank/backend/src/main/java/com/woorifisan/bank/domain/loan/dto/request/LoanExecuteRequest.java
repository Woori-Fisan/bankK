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
public class LoanExecuteRequest {

    @NotBlank
    private String loanNo;

    @NotNull
    private Long productId;

    @NotNull
    @DecimalMin("100000")
    private BigDecimal loanAmount;

    @NotNull
    @Min(1)
    private Integer repaymentPeriod;

    @NotBlank
    private String repaymentType;

    @NotBlank
    private String accountPassword;
}

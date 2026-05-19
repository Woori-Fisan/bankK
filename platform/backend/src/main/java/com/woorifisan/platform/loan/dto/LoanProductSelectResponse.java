package com.woorifisan.platform.loan.dto;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class LoanProductSelectResponse {

    private String productCode;
    private String productName;
    private BigDecimal loanAmount;
    private BigDecimal interestRate;
    private int period;
    private String repaymentType;
    private BigDecimal monthlyPayment;
}

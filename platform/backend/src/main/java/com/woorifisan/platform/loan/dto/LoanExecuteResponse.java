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
public class LoanExecuteResponse {

    private String loanId;
    private String borrowerName;
    private String depositTransactionId;
    private BigDecimal loanBalance;
    private BigDecimal executeAmount;
    private BigDecimal interestRate;
    private String repaymentStartDate;
    private String maturityDate;
}

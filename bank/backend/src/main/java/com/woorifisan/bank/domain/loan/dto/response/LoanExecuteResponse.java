package com.woorifisan.bank.domain.loan.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LoanExecuteResponse {

    private String loanNo;
    private BigDecimal loanAmount;
    private BigDecimal interestRate;
    private String repaymentType;
    private Integer repaymentPeriod;
    private BigDecimal monthlyPayment;
    private LocalDate startDate;
    private LocalDate endDate;
    private Long linkedAccountId;
}

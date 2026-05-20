package com.woorifisan.platform.domain.loan.dto.response;

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

    private String loanNo;
    private BigDecimal loanAmount;
    private String applicantName;
    private String depositAccountNo;  // 마스킹 처리
    private BigDecimal interestRate;
    private String endDate;
}

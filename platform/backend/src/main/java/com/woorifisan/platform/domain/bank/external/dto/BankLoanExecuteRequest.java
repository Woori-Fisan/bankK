package com.woorifisan.platform.domain.bank.external.dto;

import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

// Platform → Bank 대출 실행 요청 DTO
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class BankLoanExecuteRequest {

    private String loanNo;
    private Long productId;
    private BigDecimal loanAmount;
    private Integer repaymentPeriod;
    private String repaymentType;
    private String accountPassword;
}

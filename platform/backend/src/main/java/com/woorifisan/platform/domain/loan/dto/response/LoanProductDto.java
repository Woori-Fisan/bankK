package com.woorifisan.platform.domain.loan.dto.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class LoanProductDto {

    private String productCode;
    private String productName;
    private BigDecimal interestRate;
    private BigDecimal limit;
    /** 만기(개월) → 첫달 상환금액 (12/24/36개월) */
    private Map<Integer, BigDecimal> repaymentAmountByPeriod;
}

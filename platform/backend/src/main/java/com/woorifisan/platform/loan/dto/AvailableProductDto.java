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
/** 심사 승인 후 고객이 선택할 수 있는 대출 상품 1건 (LoanEvaluationResultResponse 안에 리스트로 포함) */
public class AvailableProductDto {

    /** 상품 고유 코드 (예: LP001 — 이후 계약 서류 조회 및 대출 실행 시 식별자로 사용) */
    private String loanProductCode;

    /** 화면에 표시할 상품명 (예: "우리 직장인 신용대출") */
    private String loanProductName;

    /** 이 상품으로 빌릴 수 있는 최소 금액 (원 단위) */
    private BigDecimal minAmount;

    /** 이 상품으로 빌릴 수 있는 최대 금액 (원 단위, 심사 승인 한도 이내) */
    private BigDecimal maxAmount;

    /** 연 이자율 (%, 예: 4.50 → 연 4.5%) — 부동소수점 오차 방지를 위해 BigDecimal 사용 */
    private BigDecimal interestRate;

    /** 대출 기간 (개월 단위, 예: 36 → 3년) */
    private int loanPeriodMonths;
}

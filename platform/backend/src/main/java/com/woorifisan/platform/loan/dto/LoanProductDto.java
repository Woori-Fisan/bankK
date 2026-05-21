package com.woorifisan.platform.loan.dto;

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
/** 대출 상품 상세 정보 — 기간별 월 상환금액 시뮬레이션 포함 (현재 미사용, 향후 상품 목록 조회 시 활용 예정) */
public class LoanProductDto {

    /** 상품 코드 (예: LP001) */
    private String productCode;

    /** 상품명 (예: "우리 직장인 신용대출") */
    private String productName;

    /** 연 이자율 (%) */
    private BigDecimal interestRate;

    /** 이 상품의 최대 대출 한도 (원 단위) */
    private BigDecimal limit;

    /** 대출 기간(개월) → 해당 기간의 첫 달 상환금액 매핑 (예: {12 → 870000, 24 → 450000, 36 → 310000}) */
    private Map<Integer, BigDecimal> repaymentAmountByPeriod;
}

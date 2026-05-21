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
/** 대출 상품 선택 결과 응답 (현재 미사용 — 향후 은행 코어 직접 연동 시 활용 예정) */
public class LoanProductSelectResponse {

    /** 선택된 상품 코드 */
    private String productCode;

    /** 선택된 상품명 */
    private String productName;

    /** 최종 확정된 대출 금액 (원 단위) */
    private BigDecimal loanAmount;

    /** 적용 금리 (연 %) */
    private BigDecimal interestRate;

    /** 선택된 대출 기간 (개월) */
    private int period;

    /** 상환 방식 (예: "원리금균등", "원금균등", "만기일시") */
    private String repaymentType;

    /** 매월 납부할 상환 금액 (원 단위) */
    private BigDecimal monthlyPayment;
}

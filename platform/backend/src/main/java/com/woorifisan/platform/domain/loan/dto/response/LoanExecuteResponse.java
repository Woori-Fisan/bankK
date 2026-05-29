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
/** 대출 실행 완료 응답 — 대출금 입금 처리가 끝난 후 반환되는 결과 (Step 6) */
public class LoanExecuteResponse {

    /** 생성된 대출 고유 ID (예: LOAN-XXXXXXXXXX) */
    private String loanId;

    /** 차주(대출받은 사람) 이름 */
    private String borrowerName;

    /** 대출금 입금 거래 ID — 입금된 거래를 추적할 때 사용 */
    private String depositTransactionId;

    /** 현재 대출 잔액 (원 단위, 실행 직후에는 executeAmount 와 동일) */
    private BigDecimal loanBalance;

    /** 실제 실행된 대출 금액 (원 단위) */
    private BigDecimal executeAmount;

    /** 적용 금리 (연 %) */
    private BigDecimal interestRate;

    /** 상환 기간 (개월 단위, 예: 36 → 3년) */
    private int repaymentPeriod;

    /** 매월 납부할 상환 금액 (원 단위, 원리금균등 기준) */
    private BigDecimal monthlyPayment;

    /** 첫 번째 상환일 (예: 2024-02-15) */
    private String repaymentStartDate;

    /** 대출 만기일 (예: 2027-01-15) */
    private String maturityDate;
}

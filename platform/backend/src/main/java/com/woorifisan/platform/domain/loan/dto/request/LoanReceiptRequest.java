package com.woorifisan.platform.domain.loan.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 대출 실행 확인서 PDF 생성 요청 DTO
 * 프론트엔드가 대출 실행 완료 후 보유한 모든 대출 정보를 담아 전달
 * 플랫폼은 별도 DB 없이 이 데이터만으로 PDF를 생성하고 즉시 반환
 */
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class LoanReceiptRequest {

    /** 대출 고유 ID */
    @NotBlank
    private String loanId;

    /** 차주(대출받는 사람) 이름 */
    @NotBlank
    private String borrowerName;

    /**
     * 입금 거래번호 — 대출금이 계좌에 입금될 때 생성되는 거래 식별자
     */
    @NotBlank
    private String depositTransactionId;

    /** 실제 실행된 대출 금액 (원 단위) */
    @NotNull
    private BigDecimal executeAmount;

    /** 적용 금리 */
    @NotNull
    private BigDecimal interestRate;

    /** 상환 기간 */
    @NotNull
    private Integer repaymentPeriod;

    /** 매월 납부할 원리금균등 상환금 (원 단위) */
    @NotNull
    private BigDecimal monthlyPayment;

    /** 첫 번째 상환일 */
    @NotBlank
    private String repaymentStartDate;

    /** 대출 만기일 */
    @NotBlank
    private String maturityDate;

    /**
     * 대출 상품명
     */
    @NotBlank
    private String loanProductName;

    // 입금 은행명
    @NotBlank
    private String depositBankName;

    /** 대출금이 입금될 계좌번호 */
    @NotBlank
    private String depositAccountNo;
}

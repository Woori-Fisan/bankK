package com.woorifisan.bank.domain.loan.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 대출 원장 도메인 모델.
 */
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class LoanLedger {

    private Long id;
    // 대출 계약 번호. 외부 식별자로 Platform과 공유됨
    private String loanNo;
    private Long customerId;         // customer 테이블 FK
    private Long productId;          // loan_product 테이블 FK. 심사 통과 후 상품 선택 시 확정
    private Long linkedAccountId;    // 대출금 입금 계좌 FK. 실행 단계에서 확정 (신청 시 null)

    // 신청 시점에 채워지는 필드
    private BigDecimal requestedAmount;  // 고객이 신청한 금액
    private Integer requestedPeriod;     // 신청 상환 기간 (개월)

    // 약관 동의·서류 징구 완료 여부
    private Boolean isCreditInfoAgreed;
    private Boolean isProductTermsAgreed;
    private Boolean isDocumentCollected;

    // 심사 완료 시 채워지는 필드
    private Integer appliedCreditScore;  // 심사 시점 신용점수 스냅샷
    private BigDecimal appliedDsr;       // 심사 시점 DSR(%). 총부채원리금상환비율
    private BigDecimal approvedLimit;    // 승인 한도
    private String rejectReason;         // 거절 사유. APPROVED면 null
    private LocalDateTime reviewedAt;    // 심사 처리 완료 일시

    // 대출 실행 시 채워지는 필드
    // double/float 금지 — 부동소수점 오차로 금액 계산 오류 발생. 금융 도메인은 항상 BigDecimal
    private BigDecimal loanAmount;           // 고객이 실제로 실행한 금액
    private BigDecimal currentLoanBalance;   // 현재 남은 대출 잔액
    private BigDecimal interestRate;         // 확정 금리(%)
    private String repaymentType;            // 상환 방식 (원리금균등 고정)
    private Integer repaymentPeriod;         // 확정 상환 기간 (개월)
    private LocalDate startDate;             // 대출 실행일
    private LocalDate endDate;               // 대출 만기일 (startDate + repaymentPeriod)

    // 라이프사이클
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * 신규 대출 신청 접수용 메서드
     */
    public static LoanLedger of(String loanNo, Long customerId, Long productId,
                                BigDecimal requestedAmount, Integer requestedPeriod) {
        return LoanLedger.builder()
                .loanNo(loanNo)
                .customerId(customerId)
                .productId(productId)
                .requestedAmount(requestedAmount)
                .requestedPeriod(requestedPeriod)
                .isCreditInfoAgreed(false)
                .isProductTermsAgreed(false)
                .isDocumentCollected(false)
                .status("SUBMITTED")
                .build();
    }
}

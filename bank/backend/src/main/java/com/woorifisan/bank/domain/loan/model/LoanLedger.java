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
 * 대출 원장 도메인 모델 (심사 및 실행 정보 통합)
 */
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class LoanLedger {

    private Long id;                        // 고유 ID
    private String loanNo;                  // 대출 계약 번호
    private Long customerId;                // 고객 ID (FK)
    private Long productId;                 // 대출 상품 ID (FK)
    private Long linkedAccountId;          // 대출금 입금 계좌 ID (FK)

    // 심사 요청 정보
    private BigDecimal requestedAmount;     // 신청 금액
    private Integer requestedPeriod;        // 신청 기간 (개월)

    // 약관 동의 및 서류 징구 상태
    private Boolean isCreditInfoAgreed;     // 신용 정보 동의 여부
    private Boolean isProductTermsAgreed;   // 상품 약관 동의 여부
    private Boolean isDocumentCollected;    // 서류 징구 완료 여부

    // 심사 결과 정보
    private Integer appliedCreditScore;     // 심사 시점 신용 점수
    private BigDecimal appliedDsr;          // 심사 시점 DSR (%)
    private String rejectReason;            // 거절 사유
    private LocalDateTime reviewedAt;       // 심사 완료 일시

    // 대출 실행 정보 (승인 후 확정)
    private BigDecimal loanAmount;          // 최종 대출 원금
    private BigDecimal currentLoanBalance;  // 현재 대출 잔액
    private BigDecimal interestRate;        // 확정 금리 (%)
    private String repaymentType;           // 상환 방식
    private Integer repaymentPeriod;        // 확정 상환 기간 (개월)
    private LocalDate startDate;            // 대출 실행일
    private LocalDate endDate;              // 대출 만기일

    // 상태 및 타임스탬프
    private String status;                  // 상태 (SUBMITTED, APPROVED, ACTIVE 등)
    private LocalDateTime createdAt;        // 생성 일시
    private LocalDateTime updatedAt;        // 수정 일시

    /**
     * 신규 대출 신청 처리를 위한 정적 팩토리 메서드
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

package com.woorifisan.bank.domain.loan.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.woorifisan.bank.domain.loan.model.LoanLedger;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LoanEvaluationStatusResponse {

    private Long evaluationId;
    private String loanNo;
    private String status;
    private Integer creditScore;
    private BigDecimal dsr;
    private BigDecimal approvedLimit;
    private BigDecimal interestRate;
    private String rejectReason;
    private LocalDateTime reviewedAt;

    public static LoanEvaluationStatusResponse from(LoanLedger ledger, BigDecimal approvedLimit) {
        return LoanEvaluationStatusResponse.builder()
                .evaluationId(ledger.getId())
                .loanNo(ledger.getLoanNo())
                .status(ledger.getStatus())
                .creditScore(ledger.getAppliedCreditScore())
                .dsr(ledger.getAppliedDsr())
                .approvedLimit(approvedLimit)
                .interestRate(ledger.getInterestRate())
                .rejectReason(ledger.getRejectReason())
                .reviewedAt(ledger.getReviewedAt())
                .build();
    }
}

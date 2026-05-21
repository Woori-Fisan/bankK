package com.woorifisan.bank.domain.loan.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LoanEvaluateResponse {

    private String loanNo;
    private String status;
    private String rejectReason;
    private BigDecimal approvedLimit;
    private BigDecimal interestRate;
    private Integer creditScore;
    private BigDecimal dsr;
    private List<AvailableProductDto> availableProducts;

    public static LoanEvaluateResponse approved(String loanNo, BigDecimal approvedLimit,
            BigDecimal interestRate, int creditScore, BigDecimal dsr,
            List<AvailableProductDto> availableProducts) {
        return LoanEvaluateResponse.builder()
                .loanNo(loanNo)
                .status("APPROVED")
                .approvedLimit(approvedLimit)
                .interestRate(interestRate)
                .creditScore(creditScore)
                .dsr(dsr)
                .availableProducts(availableProducts)
                .build();
    }

    public static LoanEvaluateResponse rejected(String loanNo, int creditScore,
            BigDecimal dsr, String rejectReason) {
        return LoanEvaluateResponse.builder()
                .loanNo(loanNo)
                .status("REJECTED")
                .creditScore(creditScore)
                .dsr(dsr)
                .rejectReason(rejectReason)
                .build();
    }
}

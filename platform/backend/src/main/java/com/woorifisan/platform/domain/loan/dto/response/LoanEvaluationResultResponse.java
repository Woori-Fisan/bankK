package com.woorifisan.platform.domain.loan.dto.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class LoanEvaluationResultResponse {

    private String applicationId;
    /** PENDING / APPROVED / REJECTED / FAILED */
    private String evaluationStatus;
    private String requestedAt;
    private String completedAt;

    // APPROVED 시 채워짐
    private String evaluationId;
    private BigDecimal approvedLimit;
    private BigDecimal interestRate;
    private List<AvailableProductDto> availableProducts;

    // REJECTED / FAILED 시 채워짐
    private String rejectionCode;
    private String rejectionMessage;
}

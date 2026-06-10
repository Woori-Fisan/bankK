package com.woorifisan.bank.domain.loan.dto.response;

import java.math.BigDecimal;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 대출 심사 결과 Webhook의 resPayload에 담길 민감 정보
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SensitiveLoanEvaluateResponse {
    private BigDecimal approvedLimit;
    private BigDecimal interestRate;
    private String rejectReason;
    private List<AvailableProductDto> availableProducts;
}

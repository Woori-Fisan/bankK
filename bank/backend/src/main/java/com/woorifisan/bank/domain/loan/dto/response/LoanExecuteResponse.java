package com.woorifisan.bank.domain.loan.dto.response;

import com.woorifisan.bank.global.security.dto.SecureResponse;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * 대출 실행 완료 응답 DTO (E2EE 적용)
 * 민감 정보인 customerName은 resPayload에 암호화되어 담깁니다.
 */
@Getter
@SuperBuilder
@NoArgsConstructor
public class LoanExecuteResponse extends SecureResponse {

    private String loanNo;
    private BigDecimal loanAmount;
    private BigDecimal interestRate;
    private String repaymentType;
    private Integer repaymentPeriod;
    private BigDecimal monthlyPayment;
    private LocalDate startDate;
    private LocalDate endDate;
    private Long linkedAccountId;
}

package com.woorifisan.platform.domain.bank.external.dto;

import com.woorifisan.platform.global.security.dto.SecureRequest;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Platform → Bank 대출 실행 요청 DTO (E2EE 적용)
 */
@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class BankLoanExecuteRequest extends SecureRequest {

    private String loanNo;
    private Long productId;
    private BigDecimal loanAmount;
    private Integer repaymentPeriod;
    private String repaymentType;
}

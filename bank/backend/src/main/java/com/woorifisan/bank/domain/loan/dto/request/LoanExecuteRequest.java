package com.woorifisan.bank.domain.loan.dto.request;

import com.woorifisan.bank.global.security.dto.SecureRequest;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * 암호화된 대출 실행 요청 DTO
 * 비밀번호와 계좌번호는 reqPayload 내에 암호화되어 전달됩니다.
 */
@Getter
@SuperBuilder
@NoArgsConstructor
public class LoanExecuteRequest extends SecureRequest {

    @NotBlank
    private String loanNo;

    @NotNull
    private Long productId;

    @NotNull
    @DecimalMin("100000")
    private BigDecimal loanAmount;

    @NotNull
    @Min(1)
    private Integer repaymentPeriod;

    @NotBlank
    private String repaymentType;
}

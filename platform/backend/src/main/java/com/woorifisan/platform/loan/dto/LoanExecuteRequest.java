package com.woorifisan.platform.loan.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class LoanExecuteRequest {

    @NotBlank(message = "심사 ID는 필수입니다.")
    private String evaluationId;

    /** JWE 암호화된 입금 계좌번호 (Zero-Knowledge: 플랫폼 복호화 금지) */
    @NotBlank(message = "입금 계좌번호 암호문은 필수입니다.")
    private String depositAccountNo;

    /** JWE 암호화된 계좌 비밀번호 (Zero-Knowledge: 플랫폼 복호화 금지) */
    @NotBlank(message = "계좌 비밀번호 암호문은 필수입니다.")
    private String accountPassword;

    @NotNull(message = "실행 금액은 필수입니다.")
    @Min(value = 1, message = "실행 금액은 0보다 커야 합니다.")
    private BigDecimal executeAmount;

    @Min(value = 1, message = "상환 기간은 1개월 이상이어야 합니다.")
    private int repaymentPeriod;
}

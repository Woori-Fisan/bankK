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
/**
 * 대출 실행 요청 — 계약 서류 동의 완료 후 실제로 대출금을 입금시키는 최종 단계 (Step 6)
 * 계좌번호와 비밀번호는 JWE 암호화 상태로 수신하며 플랫폼은 복호화하지 않는다 (Zero-Knowledge).
 */
public class LoanExecuteRequest {

    /** 심사 ID (SSE 결과의 evaluationId — 어떤 심사 건에 대한 실행인지 식별) */
    @NotBlank(message = "심사 ID는 필수입니다.")
    private String evaluationId;

    /** 실행할 대출 상품 코드 (Step 5에서 선택한 상품 — 어떤 상품으로 실행할지 식별) */
    @NotBlank(message = "상품 코드는 필수입니다.")
    private String loanProductCode;

    /** JWE 암호화된 입금 계좌번호 (Zero-Knowledge: 플랫폼 복호화 금지) */
    @NotBlank(message = "입금 계좌번호 암호문은 필수입니다.")
    private String depositAccountNo;

    /** JWE 암호화된 계좌 비밀번호 (Zero-Knowledge: 플랫폼 복호화 금지) */
    @NotBlank(message = "계좌 비밀번호 암호문은 필수입니다.")
    private String accountPassword;

    /** 실제로 실행할 대출 금액 (원 단위, 승인 한도 이내여야 함) */
    @NotNull(message = "실행 금액은 필수입니다.")
    @Min(value = 1, message = "실행 금액은 0보다 커야 합니다.")
    private BigDecimal executeAmount;

    /** 상환 기간 (개월 단위, 예: 36 → 3년) */
    @Min(value = 1, message = "상환 기간은 1개월 이상이어야 합니다.")
    private int repaymentPeriod;
}

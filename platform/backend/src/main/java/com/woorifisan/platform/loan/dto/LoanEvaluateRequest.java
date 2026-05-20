package com.woorifisan.platform.loan.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
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
public class LoanEvaluateRequest {

    @NotBlank(message = "은행 코드는 필수입니다.")
    private String bankCode;

    /** 프론트에서 생성한 1회용 AES 키를 은행 RSA 공개키로 암호화한 값 (Zero-Knowledge: 플랫폼 복호화 금지) */
    @NotBlank(message = "암호화된 AES 키는 필수입니다.")
    private String encryptedKey;

    /** AES로 암호화된 고객 민감 정보 (성명·주민번호·연락처 포함, Zero-Knowledge: 플랫폼 복호화 금지) */
    @NotBlank(message = "암호화된 고객 정보는 필수입니다.")
    private String encryptedCustomerInfo;

    @NotBlank(message = "입금 은행 코드는 필수입니다.")
    private String depositBankCode;

    /** AES로 암호화된 입금 계좌번호 (Zero-Knowledge: 플랫폼 복호화 금지) */
    @NotBlank(message = "암호화된 입금 계좌번호는 필수입니다.")
    private String encryptedDepositAccount;

    @NotBlank(message = "예금주명은 필수입니다.")
    private String depositAccountHolder;

    @NotNull(message = "신청 금액은 필수입니다.")
    @DecimalMin(value = "0.01", message = "신청 금액은 0보다 커야 합니다.")
    private BigDecimal requestedAmount;

    @Min(value = 1, message = "신청 기간은 1개월 이상이어야 합니다.")
    private int requestedPeriod;

    @NotEmpty(message = "약관 동의 목록은 필수입니다.")
    private List<String> agreedTermsList;
}

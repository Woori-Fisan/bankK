package com.woorifisan.platform.domain.bank.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * 출금 실행 요청 DTO
 */
@Getter
@Setter
@NoArgsConstructor
public class WithdrawalRequest {

    @NotBlank(message = "암호화된 AES 키는 필수입니다.")
    private String encryptedKey;

    @NotBlank(message = "JWS 전자서명은 필수입니다.")
    private String jwsSignature;

    @NotBlank(message = "출금 은행 코드는 필수입니다.")
    private String withdrawalBankCode;

    @NotBlank(message = "출금 계좌 번호는 필수입니다.")
    private String withdrawalAccountNo;

    @NotBlank(message = "출금 계좌 비밀번호는 필수입니다.")
    private String withdrawalPassword;

    @NotBlank(message = "고객 주민번호 앞자리는 필수입니다.")
    private String customerRrnPrefix;

    @NotNull(message = "출금 금액은 필수입니다.")
    @Positive(message = "출금 금액은 0보다 커야 합니다.")
    private BigDecimal amount;

}

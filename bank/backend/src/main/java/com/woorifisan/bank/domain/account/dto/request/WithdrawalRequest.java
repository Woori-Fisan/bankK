package com.woorifisan.bank.domain.account.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 현금 출금 요청 DTO
 */
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Schema(description = "현금 출금 요청 정보")
public class WithdrawalRequest {

    @NotBlank(message = "암호화된 AES 키는 필수입니다.")
    @Schema(description = "암호화된 AES 키 (은행 RSA 공개키로 암호화)", example = "encoded_aes_key_here")
    private String encryptedKey;

    @NotBlank(message = "JWS 전자서명은 필수입니다.")
    @Schema(description = "JWS 전자서명 (위변조 방지)", example = "eyJhbGciOiJSUzI1NiIsImtpZCI6...")
    private String jwsSignature;

    @NotBlank(message = "출금 계좌번호는 필수입니다.")
    @Schema(description = "출금 계좌번호 (일회용 AES 암호화)", example = "12345678")
    private String withdrawalAccountNo;

    @NotBlank(message = "출금 계좌 비밀번호는 필수입니다.")
    @Schema(description = "출금 계좌 비밀번호 (RSA 암호화)", example = "$2a$12$c6WGgZzJ/41BhZZePFoYSuwlQS39wBB.7JezVjnYaMmouQgVNZSwm")
    private String withdrawalPassword;

    @NotBlank(message = "고객 주민등록번호 앞 7자리는 필수입니다.")
    @Schema(description = "고객 주민등록번호 앞 7자리 (일회용 AES 암호화)", example = "0307034")
    private String customerRrnPrefix;

    @NotNull(message = "출금 요청 금액은 필수입니다.")
    @Positive(message = "출금 요청 금액은 0보다 커야 합니다.")
    @Schema(description = "출금 요청 금액", example = "50000")
    private BigDecimal amount;

}

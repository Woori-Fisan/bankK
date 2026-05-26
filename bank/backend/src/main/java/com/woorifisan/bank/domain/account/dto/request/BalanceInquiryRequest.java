package com.woorifisan.bank.domain.account.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 잔액 조회 요청 DTO
 */
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Schema(description = "잔액 조회 요청 정보")
public class BalanceInquiryRequest {

    @Schema(description = "암호화된 AES 키", example = "encryptedKey")
    @NotBlank(message = "암호화된 AES 키는 필수입니다.")
    private String encryptedKey;

    @Schema(description = "JWS 전자서명", example = "jwsSignature")
    @NotBlank(message = "JWS 전자서명은 필수입니다.")
    private String jwsSignature;

    @Schema(description = "계좌번호", example = "123456789012")
    @NotBlank(message = "출금 계좌 번호는 필수입니다.")
    private String accountNo;

    @NotBlank(message = "고객 주민번호 앞 7자리는 필수입니다.")
    @Schema(description = "고객 주민번호 앞 7자리", example = "9001011")
    private String customerRrnPrefix;

}

package com.woorifisan.platform.bank.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class BalanceInquiryRequest {

    @NotBlank(message = "암호화된 AES 키는 필수입니다.")
    private String encryptedKey;

    @NotBlank(message = "JWS 전자서명은 필수입니다.")
    private String jwsSignature;

    @NotBlank(message = "은행 코드는 필수입니다.")
    private String bankCode;

    @NotBlank(message = "계좌번호는 필수입니다.")
    private String accountNo;

    @NotBlank(message = "주민번호 앞 7자리는 필수입니다.")
    private String customerRrnPrefix;
}

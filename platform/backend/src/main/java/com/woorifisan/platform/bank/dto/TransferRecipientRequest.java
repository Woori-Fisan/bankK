package com.woorifisan.platform.bank.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 수취인 조회 요청 DTO
 */
@Getter
@Setter
@NoArgsConstructor
public class TransferRecipientRequest {

    @NotBlank(message = "암호화된 AES 키는 필수입니다.")
    private String encryptedKey;

    @NotBlank(message = "JWS 전자서명은 필수입니다.")
    private String jwsSignature;

    @NotBlank(message = "입금 은행 코드는 필수입니다.")
    private String depositBankCode;

    @NotBlank(message = "입금 계좌 번호는 필수입니다.")
    private String depositAccountNo;

}

package com.woorifisan.platform.domain.bank.dto.request;

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

    @NotBlank(message = "암호화된 전체 페이로드(JWE)는 필수입니다.")
    private String reqPayload;

    @NotBlank(message = "입금 은행 코드는 필수입니다.")
    private String depositBankCode;

}

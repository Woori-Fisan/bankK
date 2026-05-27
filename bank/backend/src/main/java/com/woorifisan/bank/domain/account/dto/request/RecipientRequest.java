package com.woorifisan.bank.domain.account.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 수취인 확인 요청 DTO
 */
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class RecipientRequest {

    @NotBlank(message = "입금 은행 코드는 필수입니다.")
    private String depositBankCode;

    @NotBlank(message = "암호화된 전체 페이로드(JWE)는 필수입니다.")
    private String reqPayload;

    @NotBlank(message = "은행 암호화 키 ID는 필수입니다.")
    private String bankKeyId;

}

package com.woorifisan.bank.domain.key.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * RSA 키 등록 요청 DTO
 */
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class BankRsaKeyRegisterRequest {

    @NotBlank(message = "공개키는 필수 입력 항목입니다.")
    private String publicKey;

    @NotBlank(message = "비밀키는 필수 입력 항목입니다.")
    private String privateKey;

}

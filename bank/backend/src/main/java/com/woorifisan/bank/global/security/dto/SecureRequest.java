package com.woorifisan.bank.global.security.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * 보안 요청을 위한 공통 베이스 DTO
 * 모든 암호화된 요청 DTO는 이 클래스를 상속받습니다.
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public abstract class SecureRequest {

    @NotBlank(message = "암호화된 전체 페이로드(JWE)는 필수입니다.")
    private String reqPayload;

    @NotBlank(message = "은행 암호화 키 ID는 필수입니다.")
    private String bankKeyId;

}

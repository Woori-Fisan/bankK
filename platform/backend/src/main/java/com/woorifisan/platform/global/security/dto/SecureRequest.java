package com.woorifisan.platform.global.security.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * 보안 요청을 위한 플랫폼 공통 베이스 DTO
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public abstract class SecureRequest {

    /**
     * 암호화된 요청 페이로드 (JWE)
     */
    private String reqPayload;

    /**
     * 은행 측 개인키 식별을 위한 ID
     */
    private String bankKeyId;

}

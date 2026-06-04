package com.woorifisan.platform.global.security.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * 보안 응답을 위한 플랫폼 공통 베이스 DTO
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public abstract class SecureResponse {

    /**
     * 암호화된 응답 페이로드 (AES-GCM)
     */
    private String resPayload;

}

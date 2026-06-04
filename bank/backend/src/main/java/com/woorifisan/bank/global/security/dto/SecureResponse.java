package com.woorifisan.bank.global.security.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * 보안 응답을 위한 공통 베이스 DTO
 * 모든 암호화된 응답 DTO는 이 클래스를 상속받습니다.
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public abstract class SecureResponse {

    /**
     * 암호화된 민감 정보 페이로드 (AES-GCM)
     */
    private String resPayload;

}

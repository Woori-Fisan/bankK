package com.woorifisan.platform.crypto.dto.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 은행 RSA 공개키 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class BankRsaKeyResponse {

    private String keyId;      // 키 고유 식별자 (Key Version)
    private String publicKey;  // 공개키 (PEM 포맷)

    public static BankRsaKeyResponse of(String keyId, String publicKey) {
        return BankRsaKeyResponse.builder()
                .keyId(keyId)
                .publicKey(publicKey)
                .build();
    }
}

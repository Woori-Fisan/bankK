package com.woorifisan.bank.domain.key.model;

import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * RSA 키 원장 도메인 모델
 */
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class BankRsaKey {

    private Long id;                // 고유 ID
    private String keyId;           // 키 고유 식별자 (Key Version)
    private String publicKey;       // 공개키 (PEM 포맷)
    private String privateKeyEnc;   // 개인키 (AES-256 암호화 저장)
    private String status;          // 상태 (ACTIVE, EXPIRED, REVOKED)
    private LocalDateTime validFrom; // 키 사용 시작 일시
    private LocalDateTime validTo;   // 키 사용 만료 일시
    private LocalDateTime createdAt; // 생성 일시
    private LocalDateTime updatedAt; // 수정 일시

    /**
     * 신규 RSA 키 등록을 위한 정적 팩토리 메서드
     */
    public static BankRsaKey of(String keyId, String publicKey, String privateKeyEnc, 
                               LocalDateTime validFrom, LocalDateTime validTo) {
        return BankRsaKey.builder()
                .keyId(keyId)
                .publicKey(publicKey)
                .privateKeyEnc(privateKeyEnc)
                .status("ACTIVE")
                .validFrom(validFrom)
                .validTo(validTo)
                .build();
    }
}

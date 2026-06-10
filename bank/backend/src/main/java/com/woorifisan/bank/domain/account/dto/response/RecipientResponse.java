package com.woorifisan.bank.domain.account.dto.response;

import com.woorifisan.bank.global.security.dto.SecureResponse;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * 수취인 확인 응답 DTO (부분 암호화 적용)
 */
@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class RecipientResponse extends SecureResponse {

    private String depositBankName; // 평문

    private String accountStatus; // 평문

    /**
     * 암호화될 민감 데이터 구조 정의
     */
    @Getter
    @Builder
    @AllArgsConstructor
    public static class SensitiveData {
        private String depositorName;
        private String depositAccountNo;
    }

    public static RecipientResponse of(String resPayload, String depositBankName, String accountStatus) {
        return RecipientResponse.builder()
                .resPayload(resPayload)
                .depositBankName(depositBankName)
                .accountStatus(accountStatus)
                .build();
    }
}


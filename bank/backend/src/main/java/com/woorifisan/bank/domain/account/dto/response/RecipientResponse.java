package com.woorifisan.bank.domain.account.dto.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 수취인 확인 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class RecipientResponse {

    private String depositorName;

    private String depositBankName;

    private String depositAccountNo;

    private String accountStatus;

    public static RecipientResponse of(String depositorName, String depositBankName, String depositAccountNo, String accountStatus) {
        return RecipientResponse.builder()
                .depositorName(depositorName)
                .depositBankName(depositBankName)
                .depositAccountNo(depositAccountNo)
                .accountStatus(accountStatus)
                .build();
    }
}

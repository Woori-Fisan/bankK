package com.woorifisan.platform.domain.bank.dto.response;

import lombok.*;

/**
 * 수취인 조회 응답 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransferRecipientResponse {

    private String depositorName;
    private String depositBankName;
    private String depositBankAccountNo;
    private String accountStatus;

}

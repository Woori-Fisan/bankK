package com.woorifisan.platform.domain.bank.dto.response;

import com.woorifisan.platform.global.security.dto.SecureResponse;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * 수취인 조회 응답 DTO (부분 암호화 적용)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class TransferRecipientResponse extends SecureResponse {

    private String depositBankName;
    private String accountStatus;

}

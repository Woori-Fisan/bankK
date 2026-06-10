package com.woorifisan.bank.domain.account.dto.decrypted;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 복호화된 이체 입금 정보 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DecryptedDepositData {

    private String depositAccountNo;
    private String withdrawalAccountNo;

}

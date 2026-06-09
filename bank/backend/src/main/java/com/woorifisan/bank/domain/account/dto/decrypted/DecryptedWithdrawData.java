package com.woorifisan.bank.domain.account.dto.decrypted;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 복호화된 이체 출금 정보 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DecryptedWithdrawData {

    private String withdrawalAccountNo;
    private String withdrawalPassword;
    private String customerRrnPrefix;
    private String customerName;
    private String depositAccountNo;

}

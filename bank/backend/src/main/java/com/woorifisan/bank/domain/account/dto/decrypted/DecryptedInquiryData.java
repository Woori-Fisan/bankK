package com.woorifisan.bank.domain.account.dto.decrypted;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 복호화된 계좌 조회 정보 데이터 클래스 (내부용)
 */
@Getter
@Setter
@NoArgsConstructor
public class DecryptedInquiryData {
    private String accountNo;
    private String customerRrnPrefix;
    private String customerName;
}

package com.woorifisan.bank.domain.account.dto.decrtypted;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 복호화된 수취인 정보 데이터 클래스 (내부용)
 */
@Getter
@Setter
@NoArgsConstructor
public class DecryptedRecipientData {
    private String depositAccountNo;
}

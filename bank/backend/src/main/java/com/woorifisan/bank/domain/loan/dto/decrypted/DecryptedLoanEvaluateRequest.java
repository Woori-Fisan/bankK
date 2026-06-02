package com.woorifisan.bank.domain.loan.dto.decrypted;

import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 복호화된 대출 심사 요청 데이터
 */
@Getter
@NoArgsConstructor
public class DecryptedLoanEvaluateRequest {

    private String customerName;       // 고객 성명
    private String customerRrnPrefix;  // 주민등록번호 앞 7자리
    private String depositAccountNo;   // 평문 계좌번호

}

package com.woorifisan.bank.domain.loan.dto.decrypted;

import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 복호화된 대출 실행 요청 데이터
 */
@Getter
@NoArgsConstructor
public class DecryptedLoanExecuteRequest {

    private String accountPassword;    // 계좌 비밀번호
    private String depositAccountNo;   // 입금 계좌번호

}

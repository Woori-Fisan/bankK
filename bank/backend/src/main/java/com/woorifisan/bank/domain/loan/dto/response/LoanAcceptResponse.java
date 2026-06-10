package com.woorifisan.bank.domain.loan.dto.response;

import com.woorifisan.bank.global.security.dto.SecureResponse;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * 대출 접수 완료 응답 DTO (E2EE 적용)
 */
@Getter
@SuperBuilder
@NoArgsConstructor
public class LoanAcceptResponse extends SecureResponse {
    private String loanNo;   // Bank가 채번한 대출 계약 번호
    private String status;   // 항상 SUBMITTED
}

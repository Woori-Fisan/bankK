package com.woorifisan.platform.domain.loan.dto.response;

import com.woorifisan.platform.global.security.dto.SecureResponse;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * 대출 심사 신청 응답 DTO (E2EE 적용)
 */
@Getter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class LoanEvaluateResponse extends SecureResponse {

    /** 은행이 발급한 대출 계약 번호 */
    private String loanNo;

    /** 접수 상태 — 항상 "SUBMITTED" */
    private String status;
}

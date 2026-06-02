package com.woorifisan.platform.domain.loan.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class LoanEvaluateResponse {

    /** 은행이 발급한 대출 계약 번호 (이후 계약 서류 조회·대출 실행 시 사용) */
    private String loanNo;

    /** 접수 상태 — 항상 "SUBMITTED" */
    private String status;
}

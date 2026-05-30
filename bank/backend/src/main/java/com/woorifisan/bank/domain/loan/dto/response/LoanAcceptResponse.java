package com.woorifisan.bank.domain.loan.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

// Bank → Platform 동기 응답 DTO. 접수 완료만 알려줌(심사 완료 x)
@Getter
@AllArgsConstructor
public class LoanAcceptResponse {
    private String loanNo;   // Bank가 채번한 대출 계약 번호. Platform이 계약서 조회 시 사용
    private String status;   // 항상 SUBMITTED — 심사 중임을 의미
}

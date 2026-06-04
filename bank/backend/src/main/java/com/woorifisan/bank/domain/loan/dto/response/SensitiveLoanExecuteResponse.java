package com.woorifisan.bank.domain.loan.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 대출 실행 시 응답 Payload에 담길 민감 정보
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SensitiveLoanExecuteResponse {
    private String customerName;
}

package com.woorifisan.platform.domain.loan.dto.request;

import com.woorifisan.platform.global.security.dto.SecureResponse;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 은행으로부터의 대출 심사 결과 Webhook 수신 DTO
 */
@Getter
@NoArgsConstructor
public class LoanCallbackRequest extends SecureResponse {

    /** SSE emitter를 찾는 키 */
    private String requestKey;

    /** 은행이 채번한 대출 계약 번호 */
    private String loanNo;

    /** 심사 결과 상태 */
    private String status;

}

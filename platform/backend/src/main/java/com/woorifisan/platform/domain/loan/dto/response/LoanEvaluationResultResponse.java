package com.woorifisan.platform.domain.loan.dto.response;

import com.woorifisan.platform.global.security.dto.SecureResponse;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.util.List;

/**
 * SSE로 push되는 심사 결과 (E2EE 적용)
 * 민감 정보는 resPayload에 암호화되어 담깁니다.
 */
@Getter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class LoanEvaluationResultResponse extends SecureResponse {

    /** 심사 상태 */
    private String evaluationStatus;

    /** 심사 고유 ID (은행 대출 계약 번호) */
    private String evaluationId;

}

package com.woorifisan.platform.domain.loan.dto.response;

import com.woorifisan.platform.global.security.dto.SecureResponse;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * 대출 실행 완료 응답 DTO (E2EE 적용)
 * 민감 정보는 resPayload에 암호화되어 전달됩니다.
 */
@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class LoanExecuteResponse extends SecureResponse {

    /** 대출 계약 번호 */
    private String loanNo;

    /** 실제로 실행된 대출 금액 */
    private BigDecimal executeAmount;

    /** 적용 금리 (연 %) */
    private BigDecimal interestRate;

    /** 상환 기간 (개월 단위) */
    private int repaymentPeriod;

    /** 매월 납부할 상환 금액 */
    private BigDecimal monthlyPayment;

    /** 상환 방식 */
    private String repaymentType;

    /** 대출 시작일 */
    private String startDate;

    /** 대출 만기일 */
    private String maturityDate;

    /** 연결된 계좌 ID */
    private Long linkedAccountId;

    /** 대출 실행 일시 (UTC ISO 8601, 예: 2026-06-08T01:30:00Z) */
    private String executedAt;
}

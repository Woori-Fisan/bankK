package com.woorifisan.platform.domain.loan.dto.request;

import com.woorifisan.platform.global.security.dto.SecureRequest;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

/**
 * 암호화된 대출 실행 요청 DTO (Pass-through)
 * 계좌번호와 비밀번호는 reqPayload 내에 암호화되어 있으며 플랫폼은 복호화하지 않습니다.
 */
@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class LoanExecuteRequest extends SecureRequest {

    /** 은행이 발급한 대출 계약번호 */
    @NotBlank(message = "대출 계약번호는 필수입니다.")
    private String loanNo;

    /** 실행할 대출 상품 ID */
    @NotNull(message = "상품 ID는 필수입니다.")
    private Long productId;

    /** 실제로 실행할 대출 금액 */
    @NotNull(message = "실행 금액은 필수입니다.")
    @Min(value = 1, message = "실행 금액은 0보다 커야 합니다.")
    private BigDecimal executeAmount;

    /** 상환 기간 (개월 단위) */
    @Min(value = 1, message = "상환 기간은 1개월 이상이어야 합니다.")
    private int repaymentPeriod;

    /** 상환 방식 (예: 원리금균등) */
    @NotBlank(message = "상환 방식은 필수입니다.")
    private String repaymentType;
}

package com.woorifisan.platform.loan.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
/** 대출 상품 선택 요청 (현재 미사용 — 향후 은행 코어 직접 연동 시 활용 예정) */
public class LoanProductSelectRequest {

    /** 대출을 실행할 은행 코드 */
    @NotBlank(message = "은행 코드는 필수입니다.")
    private String bankCode;

    /** 심사 ID (어떤 심사 건에 대한 상품 선택인지 식별) */
    @NotBlank(message = "심사 ID는 필수입니다.")
    private String evaluationId;

    /** 선택한 상품 코드 */
    @NotBlank(message = "상품 코드는 필수입니다.")
    private String productCode;

    /** 선택한 대출 기간 (개월 단위) */
    @Min(value = 1, message = "대출 기간은 1개월 이상이어야 합니다.")
    private int period;
}

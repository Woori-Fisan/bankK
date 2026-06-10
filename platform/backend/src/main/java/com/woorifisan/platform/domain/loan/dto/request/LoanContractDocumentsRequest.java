package com.woorifisan.platform.domain.loan.dto.request;

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
/** 계약 서류 조회 요청 (현재 미사용 — 향후 은행 코어 직접 연동 시 활용 예정, 현재는 PathVariable로 처리) */
public class LoanContractDocumentsRequest {

    /** 계약을 진행할 은행 코드 */
    @NotBlank(message = "은행 코드는 필수입니다.")
    private String bankCode;

    /** 심사 ID */
    @NotBlank(message = "심사 ID는 필수입니다.")
    private String evaluationId;

    /** 계약할 상품 코드 */
    @NotBlank(message = "상품 코드는 필수입니다.")
    private String productCode;
}

package com.woorifisan.platform.loan.dto;

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
public class LoanContractDocumentsRequest {

    @NotBlank(message = "은행 코드는 필수입니다.")
    private String bankCode;

    @NotBlank(message = "심사 ID는 필수입니다.")
    private String evaluationId;

    @NotBlank(message = "상품 코드는 필수입니다.")
    private String productCode;
}

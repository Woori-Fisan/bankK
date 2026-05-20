package com.woorifisan.platform.domain.loan.dto.request;

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
public class LoanProductSelectRequest {

    @NotBlank(message = "은행 코드는 필수입니다.")
    private String bankCode;

    @NotBlank(message = "심사 ID는 필수입니다.")
    private String evaluationId;

    @NotBlank(message = "상품 코드는 필수입니다.")
    private String productCode;

    @Min(value = 1, message = "대출 기간은 1개월 이상이어야 합니다.")
    private int period;
}

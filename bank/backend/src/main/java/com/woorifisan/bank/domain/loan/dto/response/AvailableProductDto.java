package com.woorifisan.bank.domain.loan.dto.response;

import com.woorifisan.bank.domain.loan.model.LoanProduct;
import java.math.BigDecimal;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AvailableProductDto {

    private Long productId;
    private String productName;
    private BigDecimal minRate;
    private BigDecimal maxRate;
    private BigDecimal minLimit;
    private BigDecimal maxLimit;
    private String conditions;
    private BigDecimal appliedRate;  // 심사에서 확정된 고객 금리

    public static AvailableProductDto from(LoanProduct product, BigDecimal appliedRate) {
        return AvailableProductDto.builder()
                .productId(product.getProductId())
                .productName(product.getProductName())
                .minRate(product.getMinRate())
                .maxRate(product.getMaxRate())
                .minLimit(product.getMinLimit())
                .maxLimit(product.getMaxLimit())
                .conditions(product.getConditions())
                .appliedRate(appliedRate)
                .build();
    }
}

package com.woorifisan.bank.domain.loan.dto.response;

import com.woorifisan.bank.domain.loan.model.LoanProduct;
import java.math.BigDecimal;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LoanProductResponse {

    private Long productId;
    private String productName;
    private BigDecimal minRate;
    private BigDecimal maxRate;
    private BigDecimal minLimit;
    private BigDecimal maxLimit;
    private String conditions;

    public static LoanProductResponse from(LoanProduct product) {
        return LoanProductResponse.builder()
                .productId(product.getProductId())
                .productName(product.getProductName())
                .minRate(product.getMinRate())
                .maxRate(product.getMaxRate())
                .minLimit(product.getMinLimit())
                .maxLimit(product.getMaxLimit())
                .conditions(product.getConditions())
                .build();
    }
}

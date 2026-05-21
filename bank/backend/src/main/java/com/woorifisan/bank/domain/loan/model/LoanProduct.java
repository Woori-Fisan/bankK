package com.woorifisan.bank.domain.loan.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 대출 상품 도메인 모델
 */
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class LoanProduct {

    private Long productId;         // 상품 ID
    private String productName;     // 상품명
    private BigDecimal minRate;     // 최저 금리 (%)
    private BigDecimal maxRate;     // 최고 금리 (%)
    private BigDecimal minLimit;    // 최소 한도
    private BigDecimal maxLimit;    // 최대 한도
    private String conditions;      // 대상 조건
    private String status;          // 상태 (ACTIVE_SALE, SUSPENDED)
    private LocalDateTime createdAt; // 생성 일시
    private LocalDateTime updatedAt; // 수정 일시

    /**
     * 신규 대출 상품 생성을 위한 정적 팩토리 메서드
     */
    public static LoanProduct of(String productName, BigDecimal minRate, BigDecimal maxRate, 
                                BigDecimal minLimit, BigDecimal maxLimit, String conditions) {
        return LoanProduct.builder()
                .productName(productName)
                .minRate(minRate)
                .maxRate(maxRate)
                .minLimit(minLimit)
                .maxLimit(maxLimit)
                .conditions(conditions)
                .status("ACTIVE_SALE")
                .build();
    }
}

package com.woorifisan.platform.loan.dto;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
/** 고객이 상품을 선택한 후 서명/동의가 필요한 계약 서류 목록 응답 (Step 5) */
public class LoanContractDocumentsResponse {

    /** 고객이 선택한 대출 상품 코드 */
    private String loanProductCode;

    /** 고객이 선택한 대출 상품명 */
    private String loanProductName;

    /** 이 상품에 대한 최종 승인 한도 (원 단위) */
    private BigDecimal approvedLimit;

    /** 서명/동의가 필요한 계약 서류 목록 (예: 대출거래약정서, 상품설명서) */
    private List<TermsDocumentDto> documents;
}

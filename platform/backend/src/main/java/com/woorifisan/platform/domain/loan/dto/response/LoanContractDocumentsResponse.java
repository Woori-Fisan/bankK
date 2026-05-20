package com.woorifisan.platform.domain.loan.dto.response;

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
public class LoanContractDocumentsResponse {

    private String loanProductCode;
    private String loanProductName;
    private BigDecimal approvedLimit;
    private String documentUrl;
    private List<TermsDocumentDto> documents;
}

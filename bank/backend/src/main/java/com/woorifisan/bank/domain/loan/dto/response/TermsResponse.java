package com.woorifisan.bank.domain.loan.dto.response;

import com.woorifisan.bank.domain.terms.model.BankTerms;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class TermsResponse {

    private String termsCode;
    private String version;
    private String title;
    private String termsUrl;
    private String termsContent;
    private Boolean isMandatory;

    public static TermsResponse from(BankTerms terms) {
        return TermsResponse.builder()
                .termsCode(terms.getTermsCode())
                .version(terms.getVersion())
                .title(terms.getTitle())
                .termsUrl(terms.getTermsUrl())
                .termsContent(terms.getTermsContent())
                .isMandatory(terms.getIsMandatory())
                .build();
    }
}

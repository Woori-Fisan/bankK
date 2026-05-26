package com.woorifisan.bank.domain.terms.model;

import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 은행 약관 도메인 모델
 */
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class BankTerms {

    private Long id;
    private String termsCode;
    private String version;
    private String title;
    private String termsUrl;
    private String termsContent;
    private Boolean isMandatory;
    private Boolean isActive;
    private String termsType;       // EVALUATION | CONTRACT
    private LocalDateTime createdAt;

    /**
     * 신규 약관 등록을 위한 정적 팩토리 메서드
     */
    public static BankTerms of(String termsCode, String version, String title, 
                              String termsUrl, Boolean isMandatory) {
        return BankTerms.builder()
                .termsCode(termsCode)
                .version(version)
                .title(title)
                .termsUrl(termsUrl)
                .isMandatory(isMandatory)
                .isActive(true)
                .build();
    }
}

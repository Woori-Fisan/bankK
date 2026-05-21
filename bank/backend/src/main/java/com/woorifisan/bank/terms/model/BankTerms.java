package com.woorifisan.bank.terms.model;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 은행 약관 도메인 모델
 */
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class BankTerms {

    private Long id;                // 고유 ID
    private String termsCode;       // 약관 고유 코드
    private String version;         // 버전
    private String title;           // 약관명
    private String termsUrl;        // WebView CDN URL
    private Boolean isMandatory;    // 필수 동의 여부
    private Boolean isActive;       // 현재 서비스 버전 여부
    private LocalDateTime createdAt; // 생성 일시

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

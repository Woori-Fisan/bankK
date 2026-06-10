package com.woorifisan.bank.domain.document.model;

import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 공통 서류 관리 도메인 모델
 */
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class CommonDocument {

    private Long id;                    // 고유 ID
    private Long loanId;                // 대출 원장 ID (FK, optional)
    private Long accountId;             // 계좌 ID (FK, optional)
    private String documentType;        // 서류 유형 (ID_CARD, INCOME_PROOF 등)
    private String originalFileName;    // 원본 파일명
    private String filePath;            // 저장 경로
    private LocalDateTime createdAt;     // 생성 일시

    /**
     * 대출 관련 서류 생성을 위한 정적 팩토리 메서드
     */
    public static CommonDocument ofLoan(Long loanId, String documentType, String originalFileName, String filePath) {
        return CommonDocument.builder()
                .loanId(loanId)
                .documentType(documentType)
                .originalFileName(originalFileName)
                .filePath(filePath)
                .build();
    }

    /**
     * 계좌 관련 서류 생성을 위한 정적 팩토리 메서드
     */
    public static CommonDocument ofAccount(Long accountId, String documentType, String originalFileName, String filePath) {
        return CommonDocument.builder()
                .accountId(accountId)
                .documentType(documentType)
                .originalFileName(originalFileName)
                .filePath(filePath)
                .build();
    }
}

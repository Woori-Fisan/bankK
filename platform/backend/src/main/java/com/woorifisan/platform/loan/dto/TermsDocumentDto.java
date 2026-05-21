package com.woorifisan.platform.loan.dto;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
/** 프론트엔드에 보여줄 약관/서류 1건의 메타 정보 (서류 조회 응답에 사용) */
public class TermsDocumentDto {

    /** 서류 유형 코드 (예: T001, T002 — LoanDocumentDto.documentType 과 매핑됨) */
    private String documentType;

    /** 화면에 표시할 서류 이름 (예: "신용정보 조회 동의서") */
    private String documentName;

    /** 서류 원문을 볼 수 있는 URL (PDF 또는 HTML) */
    private String documentUrl;

    /** 필수 동의 여부 — true이면 반드시 동의해야 심사 진행 가능 */
    private Boolean isMandatory;
}

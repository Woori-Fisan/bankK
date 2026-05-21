package com.woorifisan.platform.loan.dto;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
/** 심사 신청 전 고객에게 보여줄 필수/선택 서류 목록 응답 (Step 1) */
public class LoanRequiredDocumentsResponse {

    /** 동의 필요 서류 목록 — isMandatory=true인 항목은 반드시 동의해야 심사 진행 가능 */
    private List<TermsDocumentDto> documents;
}

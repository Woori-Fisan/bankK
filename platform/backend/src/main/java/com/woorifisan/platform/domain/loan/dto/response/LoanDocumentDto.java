package com.woorifisan.platform.domain.loan.dto.response;

import jakarta.validation.constraints.NotBlank;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
/** 프론트엔드가 약관에 동의할 때 심사 요청에 포함시키는 서류 동의 1건 */
public class LoanDocumentDto {

    /** 서류 유형 코드 (예: T001 = 신용정보 조회 동의서) */
    @NotBlank(message = "서류 유형 코드는 필수입니다.")
    private String documentType;

    /** 동의한 시각 (ISO 8601 형식, 예: 2024-01-15T10:30:00) */
    @NotBlank(message = "동의 시각은 필수입니다.")
    private String agreedAt;
}

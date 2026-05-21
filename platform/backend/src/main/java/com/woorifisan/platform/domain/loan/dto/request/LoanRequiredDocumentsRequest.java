package com.woorifisan.platform.domain.loan.dto.request;

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
/** 심사 서류 목록 조회 요청 (현재 미사용 — 향후 은행별로 다른 서류 목록을 조회할 때 활용 예정) */
public class LoanRequiredDocumentsRequest {

    /** 어느 은행의 서류 목록을 조회할지 지정하는 은행 코드 */
    @NotBlank(message = "은행 코드는 필수입니다.")
    private String bankCode;
}

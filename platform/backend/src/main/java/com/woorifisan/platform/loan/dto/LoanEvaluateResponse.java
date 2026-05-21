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
/**
 * 대출 심사 접수 응답 — 심사 요청 직후 즉시 반환
 * 심사 자체는 비동기로 진행되므로 결과는 포함되지 않는다.
 * 프론트엔드는 이 applicationId로 SSE를 구독해 심사 결과를 기다린다.
 */
public class LoanEvaluateResponse {

    /** 심사 신청 번호 (예: APP-A1B2C3D4E5F6) — SSE 구독 및 이후 단계에서 식별자로 사용 */
    private String applicationId;

    /** 접수 처리 시각 */
    private String receivedAt;
}

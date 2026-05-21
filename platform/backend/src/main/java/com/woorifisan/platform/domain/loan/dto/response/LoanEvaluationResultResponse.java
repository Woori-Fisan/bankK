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
/**
 * SSE로 push되는 심사 결과 — 상태에 따라 채워지는 필드가 다르다.
 *
 * PENDING  : applicationId, evaluationStatus, requestedAt 만 채워짐 (심사 진행 중)
 * APPROVED : evaluationId, approvedLimit, interestRate, availableProducts 추가 채워짐
 * REJECTED : rejectionCode, rejectionMessage 추가 채워짐
 * FAILED   : rejectionCode, rejectionMessage 추가 채워짐 (시스템 오류)
 */
public class LoanEvaluationResultResponse {

    /** 심사 신청 번호 (LoanEvaluateResponse.applicationId 와 동일) */
    private String applicationId;

    /** 심사 상태 — PENDING(진행중) / APPROVED(승인) / REJECTED(거절) / FAILED(오류) */
    private String evaluationStatus;

    /** 심사 요청 시각 */
    private String requestedAt;

    /** 심사 완료 시각 (PENDING 상태일 때는 null) */
    private String completedAt;

    // ── APPROVED 시에만 채워지는 필드 ──────────────────────────────

    /** 심사 고유 ID (예: EVAL-XXXXXXXXXX) — 계약 서류 조회 및 대출 실행 시 필수 */
    private String evaluationId;

    /** 최종 승인된 대출 한도 (원 단위) */
    private BigDecimal approvedLimit;

    /** 고객이 선택할 수 있는 대출 상품 목록 (각 상품의 금리는 AvailableProductDto.interestRate 참조) */
    private List<AvailableProductDto> availableProducts;

    // ── REJECTED / FAILED 시에만 채워지는 필드 ────────────────────

    /** 거절/실패 코드 */
    private String rejectionCode;

    /** 거절/실패 사유 (화면에 표시) */
    private String rejectionMessage;
}

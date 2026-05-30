package com.woorifisan.platform.domain.loan.dto.request;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.NoArgsConstructor;

// Bank → Platform Webhook 수신 DTO
// Bank의 LoanReviewAsyncService.sendWebhook()이 POST /api/v1/loan/callback 으로 전송하는 바디
@Getter
// Jackson이 역직렬화할 때 기본 생성자 + setter(없으면 필드 직접 주입)를 사용
@NoArgsConstructor
public class LoanCallbackRequest {

    // SSE emitter를 찾는 키
    private String requestKey;

    // Bank가 채번한 대출 계약 번호
    private String loanNo;

    // 심사 결과: APPROVED / REJECTED / SYSTEM_ERROR / WEBHOOK_FAILED
    private String status;

    // APPROVED일 때만 존재. 실제 실행 가능한 최대 대출 한도
    private BigDecimal approvedLimit;

    // APPROVED일 때만 존재. 신용점수 기반으로 계산된 적용 금리
    private BigDecimal interestRate;

    // REJECTED / SYSTEM_ERROR일 때만 존재. 거절 사유 문자열
    private String rejectReason;

    // APPROVED일 때만 존재. 한도·금리 조건에 맞는 상품 목록.
    private List<Map<String, Object>> availableProducts;
}

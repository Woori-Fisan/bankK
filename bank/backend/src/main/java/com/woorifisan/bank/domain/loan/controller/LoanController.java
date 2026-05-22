package com.woorifisan.bank.domain.loan.controller;

import com.woorifisan.bank.domain.loan.dto.request.LoanEvaluateRequest;
import com.woorifisan.bank.domain.loan.dto.request.LoanExecuteRequest;
import com.woorifisan.bank.domain.loan.dto.response.LoanEvaluateResponse;
import com.woorifisan.bank.domain.loan.dto.response.LoanEvaluationStatusResponse;
import com.woorifisan.bank.domain.loan.dto.response.LoanExecuteResponse;
import com.woorifisan.bank.domain.loan.dto.response.LoanProductResponse;
import com.woorifisan.bank.domain.loan.dto.response.TermsResponse;
import com.woorifisan.bank.domain.loan.service.LoanService;
import com.woorifisan.bank.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "대출 API", description = "은행 코어 대출 심사 및 실행")
@RestController
@RequestMapping("/api/v1/loan")
@RequiredArgsConstructor
public class LoanController {

    private final LoanService loanService;

    // ── 상품 목록 ─────────────────────────────────────────────────────────────

    @Operation(summary = "대출 상품 목록 조회", description = "판매 중인 대출 상품 목록을 반환합니다.")
    @GetMapping("/products")
    public ResponseEntity<ApiResponse<List<LoanProductResponse>>> getProducts() {
        return ResponseEntity.ok(ApiResponse.success(loanService.getActiveProducts()));
    }

    // ── BK-B11: 심사 서류(약관) 조회 ─────────────────────────────────────────

    @Operation(summary = "대출 심사 약관 조회 (BK-B11)",
               description = "대출 심사 단계에서 고객에게 제시할 동의서 목록을 반환합니다.")
    @GetMapping("/evaluation/terms")
    public ResponseEntity<ApiResponse<List<TermsResponse>>> getEvaluationTerms() {
        return ResponseEntity.ok(ApiResponse.success(loanService.getEvaluationTerms()));
    }

    // ── BK-B13 ~ B19: 대출 심사 요청 ────────────────────────────────────────

    @Operation(summary = "대출 심사 요청 (BK-B13~B19)",
               description = "NICE 신용점수 조회 → 600점 컷 → DSR 40% 컷 → 한도 산출 → 금리 산출 → 추천 상품 → 결과 저장")
    @PostMapping("/evaluation")
    public ResponseEntity<ApiResponse<LoanEvaluateResponse>> evaluate(
            @RequestBody @Valid LoanEvaluateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(loanService.evaluateLoan(request)));
    }

    // ── 심사 상태 Polling (플랫폼 PL-B11 연계) ───────────────────────────────

    @Operation(summary = "대출 심사 상태 조회",
               description = "evaluationId로 심사 상태를 polling합니다. APPROVED / REJECTED")
    @GetMapping("/evaluation/{evaluationId}/status")
    public ResponseEntity<ApiResponse<LoanEvaluationStatusResponse>> getEvaluationStatus(
            @PathVariable Long evaluationId) {
        return ResponseEntity.ok(ApiResponse.success(loanService.getEvaluationStatus(evaluationId)));
    }

    // ── BK-B20: 계약 서류(약관) 조회 ─────────────────────────────────────────

    @Operation(summary = "대출 계약 약관 조회 (BK-B20)",
               description = "심사 승인 후 계약 단계에서 고객에게 제시할 약관 목록을 반환합니다. evaluationId가 APPROVED여야 합니다.")
    @GetMapping("/contract/terms/{productId}/{evaluationId}")
    public ResponseEntity<ApiResponse<List<TermsResponse>>> getContractTerms(
            @PathVariable Long productId,
            @PathVariable Long evaluationId) {
        return ResponseEntity.ok(ApiResponse.success(
                loanService.getContractTerms(productId, evaluationId)));
    }

    // ── BK-B21 ~ B24, B27: 대출 실행 ────────────────────────────────────────

    @Operation(summary = "대출 실행 (BK-B21~B24, B27)",
               description = "계좌 유효성 + 비밀번호 검증 → 60일 불완전판매 방지 → 단일 트랜잭션 실행(원장갱신·잔액증가·거래기록) → 월 상환금 계산")
    @PostMapping("/execution")
    public ResponseEntity<ApiResponse<LoanExecuteResponse>> execute(
            @RequestBody @Valid LoanExecuteRequest request) {
        return ResponseEntity.ok(ApiResponse.success(loanService.executeLoan(request)));
    }
}

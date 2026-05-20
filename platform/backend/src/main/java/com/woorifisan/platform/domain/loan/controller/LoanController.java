package com.woorifisan.platform.domain.loan.controller;

import com.woorifisan.platform.global.response.ApiResponse;
import com.woorifisan.platform.domain.loan.dto.response.LoanContractDocumentsResponse;
import com.woorifisan.platform.domain.loan.dto.request.LoanEvaluateRequest;
import com.woorifisan.platform.domain.loan.dto.response.LoanEvaluateResponse;
import com.woorifisan.platform.domain.loan.dto.response.LoanEvaluationResultResponse;
import com.woorifisan.platform.domain.loan.dto.request.LoanExecuteRequest;
import com.woorifisan.platform.domain.loan.dto.response.LoanExecuteResponse;
import com.woorifisan.platform.domain.loan.dto.response.LoanRequiredDocumentsResponse;
import com.woorifisan.platform.domain.loan.service.LoanService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 대출 중개 컨트롤러 (PL-B11)
 * 모든 요청을 은행 코어로 Pass-through 라우팅 (Zero-Knowledge)
 */
@Validated
@RestController
@RequestMapping("/api/v1/loan")
@RequiredArgsConstructor
public class LoanController {

    private final LoanService loanService;

    /**
     * Step 1 — 심사 서류 조회 (BK-B11)
     * GET /api/v1/loan/review/documents
     */
    @GetMapping("/review/documents")
    public ApiResponse<LoanRequiredDocumentsResponse> getRequiredDocuments(
            @AuthenticationPrincipal Long staffId) {

        return ApiResponse.success(loanService.getRequiredDocuments(staffId));
    }

    /**
     * Step 2 — 서류 제출 및 심사 요청 (BK-B12~B19)
     * POST /api/v1/loan/evaluation
     */
    @PostMapping("/evaluation")
    public ApiResponse<LoanEvaluateResponse> evaluateLoan(
            @Valid @RequestBody LoanEvaluateRequest request,
            @AuthenticationPrincipal Long staffId) {

        return ApiResponse.success(loanService.evaluateLoan(request, staffId));
    }

    /**
     * Step 3 — 심사 결과 조회 (Polling) (BK-B19)
     * GET /api/v1/loan/evaluation/{applicationId}/status
     */
    @GetMapping("/evaluation/{applicationId}/status")
    public ApiResponse<LoanEvaluationResultResponse> getEvaluationResult(
            @PathVariable @NotBlank(message = "신청 ID는 필수입니다.") String applicationId,
            @AuthenticationPrincipal Long staffId) {

        return ApiResponse.success(loanService.getEvaluationResult(applicationId, staffId));
    }

    /**
     * Step 5 — 계약 서류 조회 (BK-B20)
     * GET /api/v1/loan/contract/documents/{loanProductCode}/{evaluationId}
     */
    @GetMapping("/contract/documents/{loanProductCode}/{evaluationId}")
    public ApiResponse<LoanContractDocumentsResponse> getContractDocuments(
            @PathVariable @NotBlank(message = "상품 코드는 필수입니다.") String loanProductCode,
            @PathVariable @NotBlank(message = "심사 ID는 필수입니다.") String evaluationId,
            @AuthenticationPrincipal Long staffId) {

        return ApiResponse.success(loanService.getContractDocuments(loanProductCode, evaluationId, staffId));
    }

    /**
     * Step 6 — 대출 실행 (BK-B21~B23)
     * POST /api/v1/loan/contract/execution
     */
    @PostMapping("/contract/execution")
    public ApiResponse<LoanExecuteResponse> executeLoan(
            @Valid @RequestBody LoanExecuteRequest request,
            @AuthenticationPrincipal Long staffId) {

        return ApiResponse.success(loanService.executeLoan(request, staffId));
    }
}

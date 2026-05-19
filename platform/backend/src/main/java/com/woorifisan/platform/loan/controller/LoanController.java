package com.woorifisan.platform.loan.controller;

import com.woorifisan.platform.global.response.ApiResponse;
import com.woorifisan.platform.loan.dto.LoanContractDocumentsRequest;
import com.woorifisan.platform.loan.dto.LoanContractDocumentsResponse;
import com.woorifisan.platform.loan.dto.LoanEvaluateRequest;
import com.woorifisan.platform.loan.dto.LoanEvaluateResponse;
import com.woorifisan.platform.loan.dto.LoanEvaluationResultResponse;
import com.woorifisan.platform.loan.dto.LoanExecuteRequest;
import com.woorifisan.platform.loan.dto.LoanExecuteResponse;
import com.woorifisan.platform.loan.dto.LoanProductSelectRequest;
import com.woorifisan.platform.loan.dto.LoanProductSelectResponse;
import com.woorifisan.platform.loan.dto.LoanRequiredDocumentsRequest;
import com.woorifisan.platform.loan.dto.LoanRequiredDocumentsResponse;
import com.woorifisan.platform.loan.service.LoanService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 대출 중개 컨트롤러 (PL-B11)
 * 모든 요청을 은행 코어로 Pass-through 라우팅 (Zero-Knowledge)
 */
@Validated
@RestController
@RequestMapping("/api/v1/bank/loan")
@RequiredArgsConstructor
public class LoanController {

    private final LoanService loanService;

    /**
     * Step 1 — 심사 서류 조회 (BK-B11)
     * 대출 심사에 필요한 공통 약관 및 동의서 목록을 조회한다.
     */
    @PostMapping("/required-documents")
    public ApiResponse<LoanRequiredDocumentsResponse> getRequiredDocuments(
            @Valid @RequestBody LoanRequiredDocumentsRequest request,
            @AuthenticationPrincipal Long staffId) {

        return ApiResponse.success(loanService.getRequiredDocuments(request, staffId));
    }

    /**
     * Step 2 — 서류 제출 및 심사 요청 (BK-B12~B19)
     * 암호문 원본을 은행 코어로 Pass-through하여 심사를 접수한다.
     */
    @PostMapping("/evaluate")
    public ApiResponse<LoanEvaluateResponse> evaluateLoan(
            @Valid @RequestBody LoanEvaluateRequest request,
            @AuthenticationPrincipal Long staffId) {

        return ApiResponse.success(loanService.evaluateLoan(request, staffId));
    }

    /**
     * Step 3 — 심사 결과 조회 (Polling) (BK-B19)
     * 동일 evaluationId에 대해 GUID를 재사용하여 중복 생성을 방지한다.
     */
    @GetMapping("/evaluation/{evaluationId}")
    public ApiResponse<LoanEvaluationResultResponse> getEvaluationResult(
            @PathVariable @NotBlank(message = "심사 ID는 필수입니다.") String evaluationId,
            @RequestParam @NotBlank(message = "은행 코드는 필수입니다.") String bankCode,
            @AuthenticationPrincipal Long staffId) {

        return ApiResponse.success(loanService.getEvaluationResult(evaluationId, bankCode, staffId));
    }

    /**
     * Step 4 — 상품 선택 (BK-B18/B27)
     * 추천 상품 중 고객이 선택한 상품과 만기를 은행 코어로 전달한다.
     */
    @PostMapping("/products/select")
    public ApiResponse<LoanProductSelectResponse> selectProduct(
            @Valid @RequestBody LoanProductSelectRequest request,
            @AuthenticationPrincipal Long staffId) {

        return ApiResponse.success(loanService.selectProduct(request, staffId));
    }

    /**
     * Step 5 — 계약 서류 조회 (BK-B20)
     * 선택한 상품의 계약 체결에 필요한 약관 목록을 조회한다.
     */
    @PostMapping("/contract-documents")
    public ApiResponse<LoanContractDocumentsResponse> getContractDocuments(
            @Valid @RequestBody LoanContractDocumentsRequest request,
            @AuthenticationPrincipal Long staffId) {

        return ApiResponse.success(loanService.getContractDocuments(request, staffId));
    }

    /**
     * Step 6 — 대출 실행 (BK-B21~B23)
     * 암호화된 계좌 비밀번호를 포함한 실행 요청을 은행 코어로 Pass-through한다.
     */
    @PostMapping("/execute")
    public ApiResponse<LoanExecuteResponse> executeLoan(
            @Valid @RequestBody LoanExecuteRequest request,
            @AuthenticationPrincipal Long staffId) {

        return ApiResponse.success(loanService.executeLoan(request, staffId));
    }
}

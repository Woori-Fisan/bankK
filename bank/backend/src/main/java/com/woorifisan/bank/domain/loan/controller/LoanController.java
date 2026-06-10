package com.woorifisan.bank.domain.loan.controller;

import com.woorifisan.bank.domain.loan.dto.request.LoanEvaluateRequest;
import com.woorifisan.bank.domain.loan.dto.request.LoanExecuteRequest;
import com.woorifisan.bank.domain.loan.dto.response.LoanAcceptResponse;
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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "대출 API", description = "은행 코어 대출 심사 및 실행")
@RestController
@RequestMapping("/api/v1/loan")
@RequiredArgsConstructor
public class LoanController {

    private final LoanService loanService;

    @Operation(summary = "대출 상품 목록 조회")
    @GetMapping("/products")
    public ResponseEntity<ApiResponse<List<LoanProductResponse>>> getProducts() {
        return ResponseEntity.ok(ApiResponse.success(loanService.getActiveProducts()));
    }

    // 심사 단계 약관 목록 반환 (동의서 체크박스에 표시될 항목들)
    @Operation(summary = "대출 심사 약관 조회 (BK-B11)")
    @GetMapping("/evaluation/terms")
    public ResponseEntity<ApiResponse<List<TermsResponse>>> getEvaluationTerms() {
        return ResponseEntity.ok(ApiResponse.success(loanService.getEvaluationTerms()));
    }

    // JSON + 파일을 동시에 받기 위해 multipart 로 선언
    @Operation(summary = "대출 접수 (BK-B13~B19)",
               description = "파일 저장 + SUBMITTED 생성 후 즉시 반환. 심사는 @Async로 백그라운드 처리 후 Platform Webhook으로 결과 통보.")
    @PostMapping(value = "/evaluation", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<LoanAcceptResponse>> accept(
            @RequestPart("data") @Valid LoanEvaluateRequest request,
            @RequestPart("files") List<MultipartFile> files) {
        return ResponseEntity.ok(ApiResponse.success(loanService.acceptLoan(request, files)));
    }

    // SSE 기반 아키텍처에서는 사용되지 않지만, 직접 상태 확인이 필요한 경우를 위해 유지
    @Operation(summary = "대출 심사 상태 조회 (Polling)")
    @GetMapping("/evaluation/{evaluationId}/status")
    public ResponseEntity<ApiResponse<LoanEvaluationStatusResponse>> getEvaluationStatus(
            @PathVariable Long evaluationId) {
        return ResponseEntity.ok(ApiResponse.success(loanService.getEvaluationStatus(evaluationId)));
    }

    // APPROVED 상태인 건에만 계약 약관을 내려줌
    @Operation(summary = "대출 계약 약관 조회 (BK-B20)")
    @GetMapping("/contract/terms/{productId}/{loanNo}")
    public ResponseEntity<ApiResponse<List<TermsResponse>>> getContractTerms(
            @PathVariable Long productId,
            @PathVariable String loanNo) {
        return ResponseEntity.ok(ApiResponse.success(
                loanService.getContractTerms(productId, loanNo)));
    }

    // @RequestBody: JSON 단일 객체로 받음 (파일 없음)
    @Operation(summary = "대출 실행 (BK-B21~B24, B27)")
    @PostMapping("/execution")
    public ResponseEntity<ApiResponse<LoanExecuteResponse>> execute(
            @RequestBody @Valid LoanExecuteRequest request) {
        return ResponseEntity.ok(ApiResponse.success(loanService.executeLoan(request)));
    }
}

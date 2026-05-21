package com.woorifisan.bank.domain.loan.controller;

import com.woorifisan.bank.domain.loan.dto.request.LoanEvaluateRequest;
import com.woorifisan.bank.domain.loan.dto.request.LoanExecuteRequest;
import com.woorifisan.bank.domain.loan.dto.response.LoanEvaluateResponse;
import com.woorifisan.bank.domain.loan.dto.response.LoanExecuteResponse;
import com.woorifisan.bank.domain.loan.dto.response.LoanProductResponse;
import com.woorifisan.bank.domain.loan.service.LoanService;
import com.woorifisan.bank.global.response.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/loan")
@RequiredArgsConstructor
public class LoanController {

    private final LoanService loanService;

    @GetMapping("/products")
    public ResponseEntity<ApiResponse<List<LoanProductResponse>>> getProducts() {
        return ResponseEntity.ok(ApiResponse.success(loanService.getActiveProducts()));
    }

    @PostMapping("/evaluate")
    public ResponseEntity<ApiResponse<LoanEvaluateResponse>> evaluate(
            @RequestBody @Valid LoanEvaluateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(loanService.evaluateLoan(request)));
    }

    @PostMapping("/execute")
    public ResponseEntity<ApiResponse<LoanExecuteResponse>> execute(
            @RequestBody @Valid LoanExecuteRequest request) {
        return ResponseEntity.ok(ApiResponse.success(loanService.executeLoan(request)));
    }
}

package com.woorifisan.bank.domain.account.controller;

import com.woorifisan.bank.domain.account.dto.request.TransactionHistoryRequest;
import com.woorifisan.bank.domain.account.dto.response.TransactionHistoryResponse;
import com.woorifisan.bank.domain.account.service.AccountService;
import com.woorifisan.bank.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 계좌 관련 API 컨트롤러
 */
@Tag(name = "계좌 (Account)", description = "계좌 조회 및 거래 내역 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/account")
public class AccountController {

    private final AccountService accountService;

    /**
     * 거래 내역 조회
     */
    @Operation(summary = "거래 내역 조회", description = "특정 계좌의 기간별 거래 내역을 조회합니다.")
    @PostMapping("/transactions")
    public ApiResponse<TransactionHistoryResponse> getTransactionHistoryList(
            @Valid @RequestBody TransactionHistoryRequest request) {
        
        TransactionHistoryResponse response = accountService.getTransactionHistoryList(request);
        
        return ApiResponse.success(response);
    }
}

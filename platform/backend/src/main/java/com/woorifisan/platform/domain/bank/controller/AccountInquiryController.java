package com.woorifisan.platform.domain.bank.controller;

import com.woorifisan.platform.domain.bank.dto.request.BalanceInquiryRequest;
import com.woorifisan.platform.domain.bank.dto.response.BalanceInquiryResponse;
import com.woorifisan.platform.domain.bank.dto.request.HistoryInquiryRequest;
import com.woorifisan.platform.domain.bank.dto.response.HistoryInquiryResponse;
import com.woorifisan.platform.domain.bank.service.AccountInquiryService;
import com.woorifisan.platform.global.config.swagger.CustomExceptionDescription;
import com.woorifisan.platform.global.config.swagger.SwaggerResponseDescription;
import com.woorifisan.platform.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Bank Inquiry", description = "은행 조회 API (잔액 및 거래내역)")
@RestController
@RequestMapping("/api/v1/bank/inquiry")
@RequiredArgsConstructor
public class AccountInquiryController {

    private final AccountInquiryService accountInquiryService;

    @Operation(summary = "잔액 조회", description = "특정 계좌의 현재 잔액을 조회합니다.")
    @CustomExceptionDescription(SwaggerResponseDescription.BANK_BALANCE)
    @PostMapping("/balance")
    public ApiResponse<BalanceInquiryResponse> getBalance(@Valid @RequestBody BalanceInquiryRequest request) {
        BalanceInquiryResponse response = accountInquiryService.getBalance(request);
        return ApiResponse.success(response);
    }

    @Operation(summary = "거래 내역 조회", description = "특정 기간 동안의 계좌 거래 내역을 조회합니다.")
    @CustomExceptionDescription(SwaggerResponseDescription.BANK_INQUIRY)
    @PostMapping("/history")
    public ApiResponse<HistoryInquiryResponse> getHistory(@Valid @RequestBody HistoryInquiryRequest request) {
        HistoryInquiryResponse response = accountInquiryService.getHistory(request);
        return ApiResponse.success(response);
    }
}

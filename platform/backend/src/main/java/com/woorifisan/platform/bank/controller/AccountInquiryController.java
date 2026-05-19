package com.woorifisan.platform.bank.controller;

import com.woorifisan.platform.bank.dto.BalanceInquiryRequest;
import com.woorifisan.platform.bank.dto.BalanceInquiryResponse;
import com.woorifisan.platform.bank.dto.HistoryInquiryRequest;
import com.woorifisan.platform.bank.dto.HistoryInquiryResponse;
import com.woorifisan.platform.bank.service.AccountInquiryService;
import com.woorifisan.platform.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/bank/inquiry")
@RequiredArgsConstructor
public class AccountInquiryController {

    private final AccountInquiryService accountInquiryService;

    @PostMapping("/balance")
    public ApiResponse<BalanceInquiryResponse> getBalance(@Valid @RequestBody BalanceInquiryRequest request) {
        BalanceInquiryResponse response = accountInquiryService.getBalance(request);
        return ApiResponse.success(response);
    }

    @PostMapping("/history")
    public ApiResponse<HistoryInquiryResponse> getHistory(@Valid @RequestBody HistoryInquiryRequest request) {
        HistoryInquiryResponse response = accountInquiryService.getHistory(request);
        return ApiResponse.success(response);
    }
}
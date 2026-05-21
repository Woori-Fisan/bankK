package com.woorifisan.bank.domain.account.controller;

import com.woorifisan.bank.domain.account.dto.request.BalanceInquiryRequest;
import com.woorifisan.bank.domain.account.dto.response.BalanceInquiryResponse;
import com.woorifisan.bank.domain.account.service.AccountService;
import com.woorifisan.bank.global.response.ApiResponse;
import com.woorifisan.bank.global.swagger.CustomExceptionDescription;
import com.woorifisan.bank.global.swagger.SwaggerResponseDescription;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Account", description = "계좌 API")
@RestController
@RequestMapping("/api/v1/baas/account")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @Operation(summary = "잔액 조회", description = "계좌번호와 주민번호 앞자리를 이용해 잔액을 조회합니다.")
    @CustomExceptionDescription(SwaggerResponseDescription.ACCOUNT_INQUIRY)
    @PostMapping("/balance")
    public ApiResponse<BalanceInquiryResponse> getBalance(@Valid @RequestBody BalanceInquiryRequest request) {
        return ApiResponse.success(accountService.getBalance(request));
    }
}

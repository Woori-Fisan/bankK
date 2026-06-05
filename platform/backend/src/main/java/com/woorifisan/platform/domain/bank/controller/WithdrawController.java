package com.woorifisan.platform.domain.bank.controller;

import com.woorifisan.platform.domain.bank.dto.request.WithdrawalRequest;
import com.woorifisan.platform.domain.bank.dto.response.TransferResponse;
import com.woorifisan.platform.domain.bank.service.WithdrawalService;
import com.woorifisan.platform.global.config.resolver.CurrentUser;
import com.woorifisan.platform.global.config.swagger.CustomExceptionDescription;
import com.woorifisan.platform.global.config.swagger.SwaggerResponseDescription;
import com.woorifisan.platform.global.response.ApiResponse;
import com.woorifisan.platform.global.security.annotation.VerifyTerminalSignature;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Bank Withdraw", description = "은행 출금 API")
@RestController
@RequestMapping("/api/v1/bank/withdrawals")
@RequiredArgsConstructor
public class WithdrawController {

    private final WithdrawalService withdrawalService;

    @Operation(summary = "출금 실행", description = "대행기관 직원이 고객의 출금 요청을 실행합니다. (E2EE 암호문 Pass-through)")
    @CustomExceptionDescription(SwaggerResponseDescription.BANK_WITHDRAW)
    @VerifyTerminalSignature
    @PostMapping
    public ApiResponse<TransferResponse> executeWithdraw(
            @Parameter(hidden = true) @CurrentUser Long staffId,
            @RequestHeader("x-jws-signature") String jwsSignature,
            @RequestHeader("x-bank-key-id") String bankKeyId,
            @Valid @RequestBody WithdrawalRequest request) {
        TransferResponse response = withdrawalService.executeWithdraw(request, bankKeyId, staffId);
        return ApiResponse.success(response);
    }
}
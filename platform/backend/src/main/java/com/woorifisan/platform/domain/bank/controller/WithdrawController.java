package com.woorifisan.platform.domain.bank.controller;

import com.woorifisan.platform.domain.bank.dto.request.WithdrawalRequest;
import com.woorifisan.platform.domain.bank.dto.response.TransferResponse;
import com.woorifisan.platform.domain.bank.service.WithdrawalService;
import com.woorifisan.platform.global.config.resolver.CurrentUser;
import com.woorifisan.platform.global.config.swagger.CustomExceptionDescription;
import com.woorifisan.platform.global.config.swagger.SwaggerResponseDescription;
import com.woorifisan.platform.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Bank Withdraw", description = "은행 출금 API")
@RestController
@RequestMapping("/api/v1/bank/withdrawals")
@RequiredArgsConstructor
public class WithdrawController {

    private final WithdrawalService withdrawalService;

    @Operation(summary = "출금 실행", description = "대행기관 직원이 고객의 출금 요청을 실행합니다.")
    @CustomExceptionDescription(SwaggerResponseDescription.BANK_WITHDRAW)
    @PostMapping
    public ApiResponse<TransferResponse> executeWithdraw(
            @Parameter(hidden = true) @CurrentUser Long staffId,
//            @RequestHeader(value = "x-jws-signature") String jwsSignature,
            @Valid @RequestBody WithdrawalRequest request) {
        TransferResponse response = withdrawalService.executeWithdraw(request);
        return ApiResponse.success(response);
    }
}
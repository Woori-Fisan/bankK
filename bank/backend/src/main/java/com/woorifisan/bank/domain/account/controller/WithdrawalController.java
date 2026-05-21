package com.woorifisan.bank.domain.account.controller;

import com.woorifisan.bank.domain.account.dto.request.WithdrawalRequest;
import com.woorifisan.bank.domain.account.dto.response.WithdrawalResponse;
import com.woorifisan.bank.domain.account.service.WithdrawalService;
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

@Tag(name = "Withdrawal", description = "출금 API")
@RestController
@RequestMapping("/api/v1/baas/withdrawal")
@RequiredArgsConstructor
public class WithdrawalController {

    private final WithdrawalService withdrawalService;

    @Operation(summary = "현금 출금", description = "계좌에서 현금을 출금합니다. (보안 필드 포함)")
    @CustomExceptionDescription(SwaggerResponseDescription.ACCOUNT_WITHDRAWAL)
    @PostMapping("/withdrawal")
    public ApiResponse<WithdrawalResponse> withdraw(@Valid @RequestBody WithdrawalRequest request) {
        return ApiResponse.success(withdrawalService.withdraw(request));
    }

}

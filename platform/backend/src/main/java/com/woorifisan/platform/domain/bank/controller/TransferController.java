package com.woorifisan.platform.domain.bank.controller;

import com.woorifisan.platform.domain.bank.dto.request.TransferRecipientRequest;
import com.woorifisan.platform.domain.bank.dto.response.TransferRecipientResponse;
import com.woorifisan.platform.domain.bank.dto.request.TransferRequest;
import com.woorifisan.platform.domain.bank.dto.response.TransferResponse;
import com.woorifisan.platform.domain.bank.service.TransferService;
import com.woorifisan.platform.global.config.swagger.CustomExceptionDescription;
import com.woorifisan.platform.global.config.swagger.SwaggerResponseDescription;
import com.woorifisan.platform.global.response.ApiResponse;
import com.woorifisan.platform.global.security.annotation.VerifyTerminalSignature;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 이체 API 컨트롤러
 * - 수취인 조회 및 이체 실행 중계 역할을 수행함
 */
@Tag(name = "Bank Transfer", description = "은행 이체 API")
@RestController
@RequestMapping("/api/v1/bank/transfer")
@RequiredArgsConstructor
public class TransferController {

    private final TransferService transferService;

    @Operation(summary = "수취인 조회", description = "이체 전 수취인의 계좌 정보 및 성명을 확인합니다.")
    @CustomExceptionDescription(SwaggerResponseDescription.BANK_TRANSFER)
    @VerifyTerminalSignature
    @PostMapping("/recipient")
    public ApiResponse<TransferRecipientResponse> getRecipient(
            @RequestHeader("x-jws-signature") String jwsSignature,
            @RequestHeader("x-bank-key-id") String bankKeyId,
            @RequestBody @Valid TransferRecipientRequest request) {
        TransferRecipientResponse response = transferService.getRecipient(request, bankKeyId);
        return ApiResponse.success(response);
    }

    @Operation(summary = "이체 실행", description = "대행기관 직원이 고객의 이체 요청을 실행합니다.")
    @CustomExceptionDescription(SwaggerResponseDescription.BANK_TRANSFER)
    @VerifyTerminalSignature
    @PostMapping
    public ApiResponse<TransferResponse> executeTransfer(
            @RequestHeader("x-jws-signature") String jwsSignature,
            @RequestHeader("x-withdraw-key-id") String withdrawKeyId,
            @RequestHeader("x-deposit-key-id") String depositKeyId,
            @RequestBody @Valid TransferRequest request,
            @AuthenticationPrincipal Long staffId) {
        TransferResponse response = transferService.executeTransfer(request, jwsSignature, withdrawKeyId, depositKeyId, staffId);
        return ApiResponse.success(response);
    }

}

package com.woorifisan.platform.domain.bank.controller;

import com.woorifisan.platform.domain.bank.dto.request.TransferRecipientRequest;
import com.woorifisan.platform.domain.bank.dto.response.TransferRecipientResponse;
import com.woorifisan.platform.domain.bank.dto.request.TransferRequest;
import com.woorifisan.platform.domain.bank.dto.response.TransferResponse;
import com.woorifisan.platform.domain.bank.service.TransferService;
import com.woorifisan.platform.global.config.swagger.CustomExceptionDescription;
import com.woorifisan.platform.global.config.swagger.SwaggerResponseDescription;
import com.woorifisan.platform.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
    @PostMapping("/recipient")
    public ApiResponse<TransferRecipientResponse> getRecipient(@RequestBody @Valid TransferRecipientRequest request) {
        TransferRecipientResponse response = transferService.getRecipient(request);
        return ApiResponse.success(response);
    }

    @Operation(summary = "이체 실행", description = "대행기관 직원이 고객의 이체 요청을 실행합니다.")
    @CustomExceptionDescription(SwaggerResponseDescription.BANK_TRANSFER)
    @PostMapping
    public ApiResponse<TransferResponse> executeTransfer(@RequestBody @Valid TransferRequest request) {
        TransferResponse response = transferService.executeTransfer(request);
        return ApiResponse.success(response);
    }

}

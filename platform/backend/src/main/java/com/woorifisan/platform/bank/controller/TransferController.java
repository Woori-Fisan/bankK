package com.woorifisan.platform.bank.controller;

import com.woorifisan.platform.bank.dto.TransferRecipientRequest;
import com.woorifisan.platform.bank.dto.TransferRecipientResponse;
import com.woorifisan.platform.bank.dto.TransferRequest;
import com.woorifisan.platform.bank.dto.TransferResponse;
import com.woorifisan.platform.bank.service.TransferService;
import com.woorifisan.platform.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 이체 API 컨트롤러
 * - 수취인 조회 및 이체 실행 중계 역할을 수행함
 */
@RestController
@RequestMapping("/api/v1/bank/transfer")
@RequiredArgsConstructor
public class TransferController {

    private final TransferService transferService;

    @PostMapping("/recipient")
    public ApiResponse<TransferRecipientResponse> getRecipient(@RequestBody @Valid TransferRecipientRequest request) {
        TransferRecipientResponse response = transferService.getRecipient(request);
        return ApiResponse.success(response);
    }

    @PostMapping
    public ApiResponse<TransferResponse> executeTransfer(@RequestBody @Valid TransferRequest request) {
        TransferResponse response = transferService.executeTransfer(request);
        return ApiResponse.success(response);
    }

}

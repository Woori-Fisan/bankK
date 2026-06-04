package com.woorifisan.bank.domain.account.controller;

import com.woorifisan.bank.domain.account.dto.request.DepositRequest;
import com.woorifisan.bank.domain.account.dto.request.RecipientRequest;
import com.woorifisan.bank.domain.account.dto.request.TransferRequest;
import com.woorifisan.bank.domain.account.dto.response.RecipientResponse;
import com.woorifisan.bank.domain.account.dto.response.TransferResponse;
import com.woorifisan.bank.domain.account.service.TransferService;
import com.woorifisan.bank.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * BaaS 이체 컨트롤러
 */
@Tag(name = "BaaS Transfer API", description = "은행 이체 및 수취인 확인 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/baas/transfer")
public class TransferController {

    private final TransferService transferService;

    /**
     * 출금 이체 실행 (타행 이체용)
     * @param request 출금 이체 요청 정보
     * @return 출금 결과
     */
    @Operation(summary = "출금 이체 실행 (타행 이체용)", description = "타행 이체를 위해 당행 계좌에서 금액을 출금합니다.")
    @PostMapping("/withdraw")
    public ApiResponse<TransferResponse> withdrawTransfer(@RequestBody @Valid TransferRequest request) {
        TransferResponse response = transferService.withdrawTransfer(request);
        return ApiResponse.success(response);
    }

    /**
     * 입금 이체 실행 (타행 이체용)
     * @param request 입금 이체 요청 정보
     * @return 입금 결과
     */
    @Operation(summary = "입금 이체 실행 (타행 이체용)", description = "타행에서 넘어온 금액을 당행 계좌에 입금합니다.")
    @PostMapping("/deposit")
    public ApiResponse<TransferResponse> depositTransfer(@RequestBody @Valid DepositRequest request) {
        TransferResponse response = transferService.depositTransfer(request);
        return ApiResponse.success(response);
    }

    /**
     * 이체 환불 실행 (입금 실패 시 복구용)
     * @param request 출금 시 사용했던 이체 요청 정보
     * @return 환불 결과
     */
    @Operation(summary = "이체 환불 실행 (입금 실패 시 복구용)", description = "입금 단계 실패 시 출금되었던 금액을 원래 계좌로 환불합니다.")
    @PostMapping("/refund")
    public ApiResponse<TransferResponse> refundTransfer(@RequestBody @Valid TransferRequest request) {
        TransferResponse response = transferService.refundTransfer(request);
        return ApiResponse.success(response);
    }

    /**
     * 수취인 확인
     * @param request 수취인 확인 요청 정보
     * @return 수취인 정보
     */
    @Operation(summary = "수취인 확인", description = "이체 전 수취인의 성명을 확인합니다.")
    @PostMapping("/recipient")
    public ApiResponse<RecipientResponse> verifyRecipient(@RequestBody @Valid RecipientRequest request) {
        RecipientResponse response = transferService.verifyRecipient(request);
        return ApiResponse.success(response);
    }
}

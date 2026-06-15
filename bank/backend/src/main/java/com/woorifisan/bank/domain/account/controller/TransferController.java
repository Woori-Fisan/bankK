package com.woorifisan.bank.domain.account.controller;

import com.woorifisan.bank.domain.account.dto.request.InternalDepositRequest;
import com.woorifisan.bank.domain.account.dto.request.RecipientRequest;
import com.woorifisan.bank.domain.account.dto.request.TransferRequest;
import com.woorifisan.bank.domain.account.dto.response.RecipientResponse;
import com.woorifisan.bank.domain.account.dto.response.TransferResponse;
import com.woorifisan.bank.domain.account.dto.response.TransferStatusResponse;
import com.woorifisan.bank.domain.account.service.TransferService;
import com.woorifisan.bank.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
     * 통합 이체 실행 (BaaS용)
     * 당행 이체: 즉시 SUCCESS 반환.
     * 타행 이체: PENDING 상태로 즉시 반환 → 클라이언트가 /transfer/status/{txId}로 폴링.
     */
    @Operation(summary = "통합 이체 실행 (BaaS용)", description = "출금부터 입금(당행/타행)까지 한 번에 처리하는 통합 이체 API입니다.")
    @PostMapping("/execute")
    public ApiResponse<TransferResponse> executeTransfer(
            @RequestBody @Valid TransferRequest request) {
        TransferResponse response = transferService.executeTransfer(request);
        return ApiResponse.success(response);
    }

    /**
     * 내부 입금 실행 (은행 간 통신용)
     * @param request 내부 입금 요청 정보
     * @return 입금 결과
     */
    @Operation(summary = "내부 입금 실행 (은행 간 통신용)", description = "타행에서 보낸 입금 요청을 처리합니다. 암호화 없이 직접 데이터를 수신합니다.")
    @PostMapping("/internal/deposit")
    public ApiResponse<TransferResponse> internalDeposit(@RequestBody @Valid InternalDepositRequest request) {
        TransferResponse response = transferService.internalDeposit(request);
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

    /**
     * 타행 거래 상태 조회 (이중 지급 방지용)
     * @param txId 조회할 거래 트랜잭션 ID
     * @return 거래 상태 정보
     */
    @Operation(summary = "타행 거래 상태 조회 (이중 지급 방지용)", description = "출금 은행에서 생성된 트랜잭션 ID를 기반으로 해당 거래의 입금 처리 상태를 조회합니다.")
    @GetMapping("/status/{txId}")
    public ApiResponse<TransferStatusResponse> getTransferStatus(@PathVariable("txId") String txId) {
        TransferStatusResponse response = transferService.getTransferStatus(txId);
        return ApiResponse.success(response);
    }
}
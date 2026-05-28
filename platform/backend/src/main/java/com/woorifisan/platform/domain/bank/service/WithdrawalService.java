package com.woorifisan.platform.domain.bank.service;

import com.woorifisan.platform.domain.bank.dto.request.WithdrawalRequest;
import com.woorifisan.platform.domain.bank.dto.response.TransferResponse;
import com.woorifisan.platform.domain.bank.external.client.BankExternalClient;
import com.woorifisan.platform.domain.bank.external.dto.BankWithdrawalRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class WithdrawalService {

    private final BankExternalClient bankExternalClient;

    /**
     * 출금 실행 (E2EE Zero-Knowledge Pass-through)
     *
     * @param request 출금 요청 정보
     * @param bankKeyId 은행 키 ID
     * @return 출금 결과 정보
     */
    @Transactional
    public TransferResponse executeWithdraw(WithdrawalRequest request, String bankKeyId) {
        log.info("출금 실행 요청 중계 - 은행코드: {}, 키ID: {}, 금액: {}",
                request.getWithdrawalBankCode(), bankKeyId, request.getAmount());

        // 1. 외부 은행 전용 요청 DTO로 변환 (Pass-through)
        BankWithdrawalRequest bankRequest = BankWithdrawalRequest.of(
                request.getReqPayload(),
                bankKeyId,
                request.getAmount()
        );

        // 2. 외부 은행 API 호출 및 결과 반환
        return bankExternalClient.withdraw(request.getWithdrawalBankCode(), bankRequest);
    }
}

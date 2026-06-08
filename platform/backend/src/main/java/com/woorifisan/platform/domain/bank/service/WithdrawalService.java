package com.woorifisan.platform.domain.bank.service;

import com.woorifisan.platform.domain.bank.dto.request.WithdrawalRequest;
import com.woorifisan.platform.domain.bank.dto.response.TransferResponse;
import com.woorifisan.platform.domain.bank.external.client.BankExternalClient;
import com.woorifisan.platform.domain.bank.external.dto.BankWithdrawalRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WithdrawalService {

    private final BankExternalClient bankExternalClient;

    /**
     * 출금 실행 (E2EE Pass-through)
     *
     * @param request 출금 요청 정보 (reqPayload 포함)
     * @param bankKeyId 헤더에서 추출된 은행 키 ID
     * @return 출금 결과 정보
     */
    public TransferResponse executeWithdraw(WithdrawalRequest request, String bankKeyId) {
        // 1. 외부 은행 코어로 전달할 요청 DTO 생성 (Zero-Knowledge Pass-through)
        BankWithdrawalRequest bankRequest = BankWithdrawalRequest.of(
                request.getReqPayload(),
                bankKeyId,
                request.getAmount()
        );

        return bankExternalClient.withdraw(request.getWithdrawalBankCode(), bankRequest);
    }
}

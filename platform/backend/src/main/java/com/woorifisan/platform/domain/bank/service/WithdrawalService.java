package com.woorifisan.platform.domain.bank.service;

import com.woorifisan.platform.domain.bank.dto.request.WithdrawalRequest;
import com.woorifisan.platform.domain.bank.dto.response.TransferResponse;
import com.woorifisan.platform.domain.bank.external.client.BankExternalClient;
import com.woorifisan.platform.domain.bank.external.dto.BankWithdrawalRequest;
import com.woorifisan.platform.global.lock.TxSessionLockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class WithdrawalService {

    private final BankExternalClient bankExternalClient;
    private final TxSessionLockService sessionLockService;

    /**
     * 출금 실행 (E2EE Pass-through)
     *
     * @param request 출금 요청 정보 (reqPayload 포함)
     * @param bankKeyId 헤더에서 추출된 은행 키 ID
     * @param staffId 요청 직원 ID (세션 락 키로 사용)
     * @return 출금 결과 정보
     */
    @Transactional
    public TransferResponse executeWithdraw(WithdrawalRequest request, String bankKeyId, Long staffId) {
        log.info("현금 출금 요청 중계 - 은행코드: {}, 키ID: {}, staffId: {}",
                request.getWithdrawalBankCode(), bankKeyId, staffId);

        sessionLockService.acquireLock(staffId);
        try {
            // 1. 외부 은행 코어로 전달할 요청 DTO 생성 (Zero-Knowledge Pass-through)
            BankWithdrawalRequest bankRequest = BankWithdrawalRequest.of(
                    request.getReqPayload(),
                    bankKeyId,
                    request.getAmount()
            );

            // 2. 은행 코어 호출 직전 PROCESSING으로 전환
            sessionLockService.upgradeToProcessing(staffId);

            // 3. 외부 클라이언트를 통해 은행 코어 API 호출 및 결과 직접 반환
            return bankExternalClient.withdraw(request.getWithdrawalBankCode(), bankRequest);
        } finally {
            sessionLockService.releaseLock(staffId);
        }
    }
}

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
     * 출금 실행
     *
     * @param request 출금 요청 정보
     * @return 출금 결과 정보
     */
    @Transactional
    public TransferResponse executeWithdraw(WithdrawalRequest request) {
        log.info("출금 실행 요청 - 은행: {}, 계좌: {}, 금액: {}",
                request.getWithdrawalBankCode(), request.getWithdrawalAccountNo(), request.getAmount());

        // 1. 외부 은행 전용 요청 DTO로 변환 (비밀번호는 평문으로 전달)
        BankWithdrawalRequest bankRequest = BankWithdrawalRequest.of(
                request.getEncryptedKey(),
                "jwsSignature",
                request.getWithdrawalAccountNo(),
                request.getWithdrawalPassword(),
                request.getCustomerRrnPrefix(),
                request.getAmount()
        );

        // 2. 외부 은행 API 호출 및 결과 반환
        return bankExternalClient.withdraw(request.getWithdrawalBankCode(), bankRequest);
    }
}

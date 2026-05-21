package com.woorifisan.bank.domain.account.service;

import com.woorifisan.bank.domain.account.dto.request.BalanceInquiryRequest;
import com.woorifisan.bank.domain.account.dto.response.BalanceInquiryResponse;
import com.woorifisan.bank.domain.account.mapper.AccountMapper;
import com.woorifisan.bank.domain.account.model.Account;
import com.woorifisan.bank.global.exception.BusinessException;
import com.woorifisan.bank.global.response.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountMapper accountMapper;

    @Transactional(readOnly = true)
    public BalanceInquiryResponse getBalance(BalanceInquiryRequest request) {
        // 1. 계좌 및 고객 정보 검증 쿼리 호출
        // (참고: 실제 환경에서는 request.getAccountNo()를 해싱하고 request.getCustomerRrnPrefix()를 암호화하여 전달해야 함)
        Account account = accountMapper.findByAccountNoHashAndRrnPrefix(
                request.getAccountNo(),      // 예시를 위해 평문 그대로 사용 (실제론 Hash)
                request.getCustomerRrnPrefix() // 예시를 위해 평문 그대로 사용 (실제론 Enc)
        ).orElseThrow(() -> new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND));

        // 2. 응답 반환
        return BalanceInquiryResponse.builder()
                .balance(account.getBalance())
                .status(account.getStatus())
                .build();
    }
}

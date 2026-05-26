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
        Account account = accountMapper.findByAccountNoAndRrnPrefix(
                request.getAccountNo(),
                request.getCustomerRrnPrefix()
        ).orElseThrow(() -> new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND));

        // 2. 응답 반환
        return BalanceInquiryResponse.builder()
                .balance(account.getBalance())
                .status(account.getStatus())
                .build();
    }
}

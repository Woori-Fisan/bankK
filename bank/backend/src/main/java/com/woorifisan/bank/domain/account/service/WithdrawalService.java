package com.woorifisan.bank.domain.account.service;

import com.woorifisan.bank.domain.account.dto.request.WithdrawalRequest;
import com.woorifisan.bank.domain.account.dto.response.WithdrawalResponse;
import com.woorifisan.bank.domain.account.mapper.AccountMapper;
import com.woorifisan.bank.domain.account.mapper.TransactionLedgerMapper;
import com.woorifisan.bank.domain.account.model.Account;
import com.woorifisan.bank.domain.account.model.TransactionLedger;
import com.woorifisan.bank.global.exception.BusinessException;
import com.woorifisan.bank.global.response.ErrorCode;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class WithdrawalService {

    private final AccountMapper accountMapper;
    private final TransactionLedgerMapper transactionLedgerMapper;

    @Transactional
    public WithdrawalResponse withdraw(WithdrawalRequest request) {
        // 1. 계좌 및 고객 정보 검증
        Account account = accountMapper.findByAccountNoHashAndRrnPrefix(
                request.getWithdrawalAccountNo(),
                request.getCustomerRrnPrefix()
        ).orElseThrow(() -> new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND));

        // 2. 출금 가능 여부 체크
        validateWithdrawal(account, request.getAmount());

        // 3. 거래 내역 생성
        String txId = "TXW-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        BigDecimal balanceAfter = account.getBalance().subtract(request.getAmount());
        
        TransactionLedger ledger = TransactionLedger.of(
                txId,
                account.getId(),
                "WITHDRAW",
                request.getAmount(),
                balanceAfter,
                "현금 출금",
                "SUCCESS"
        );
        transactionLedgerMapper.insert(ledger);

        // 4. 원장 업데이트 (낙관적 락)
        int updatedRows = accountMapper.subtractBalance(account.getId(), request.getAmount(), account.getVersion());
        if (updatedRows == 0) {
            throw new BusinessException(ErrorCode.CONCURRENT_MODIFICATION);
        }

        log.info("출금 완료: 계좌={}, 금액={}, 잔액={}", account.getAccountNoHash(), request.getAmount(), balanceAfter);

        return WithdrawalResponse.builder()
                .transactionId(txId)
                .balanceAfter(balanceAfter)
                .transactionDate(ledger.getTransactedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .build();
    }

    private void validateWithdrawal(Account account, BigDecimal amount) {
        if (!"SAVINGS".equals(account.getAccountType())) {
            throw new BusinessException(ErrorCode.INVALID_ACCOUNT_TYPE);
        }

        if (!"NORMAL".equals(account.getStatus())) {
            throw new BusinessException(ErrorCode.ACCOUNT_NOT_NORMAL);
        }

        if (account.getBalance().compareTo(amount) < 0) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_BALANCE);
        }
    }
}

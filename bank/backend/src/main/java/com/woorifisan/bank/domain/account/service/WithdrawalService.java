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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class WithdrawalService {

    private final AccountMapper accountMapper;
    private final TransactionLedgerMapper transactionLedgerMapper;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public WithdrawalResponse withdraw(WithdrawalRequest request) {
        // 1. 계좌 및 고객 정보 검증
        Account account = accountMapper.findByAccountNoAndRrnPrefix(
                request.getWithdrawalAccountNo(),
                request.getCustomerRrnPrefix()
        ).orElseThrow(() -> new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND));

        // 2. 출금 가능 여부 체크 (이미 해싱된 비밀번호 비교)
        validateWithdrawal(account, request.getWithdrawalPassword(), request.getAmount());

        // 3. 거래 내역 생성
        String txId = "TXW-" + UUID.randomUUID().toString().replace("-", "").toUpperCase();
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
            Account currentAccount = accountMapper.findById(account.getId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND));

            if (currentAccount.getBalance().compareTo(request.getAmount()) < 0) {
                throw new BusinessException(ErrorCode.INSUFFICIENT_BALANCE);
            }
            throw new BusinessException(ErrorCode.CONCURRENT_MODIFICATION);
        }

        log.info("출금 완료: 계좌={}, 금액={}, 잔액={}", account.getAccountNo(), request.getAmount(), balanceAfter);

        return WithdrawalResponse.builder()
                .transactionId(txId)
                .balanceAfter(balanceAfter)
                .transactionDate(ledger.getTransactedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")))
                .build();
    }

    private void validateWithdrawal(Account account, String inputPassword, BigDecimal amount) {
        // 1. 계좌 유형 및 상태 확인
        if (!"DEPOSIT".equals(account.getAccountType())) {
            throw new BusinessException(ErrorCode.INVALID_ACCOUNT_TYPE);
        }

        if (!"NORMAL".equals(account.getStatus())) {
            throw new BusinessException(ErrorCode.ACCOUNT_NOT_NORMAL);
        }

        // 2. 비밀번호 검증
        log.info("비밀번호 검증: 입력={}, 저장={}", inputPassword, account.getPassword());
        if (!passwordEncoder.matches(inputPassword, account.getPassword())) {
            throw new BusinessException(ErrorCode.ACCOUNT_PW_ERROR);
        }

        // 3. 잔액 확인
        if (account.getBalance().compareTo(amount) < 0) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_BALANCE);
        }
    }
}

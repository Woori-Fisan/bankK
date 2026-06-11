package com.woorifisan.bank.domain.account.service;

import com.woorifisan.bank.domain.account.dto.decrypted.DecryptedWithdrawData;
import com.woorifisan.bank.domain.account.dto.request.WithdrawalRequest;
import com.woorifisan.bank.domain.account.dto.response.WithdrawalResponse;
import com.woorifisan.bank.domain.account.mapper.AccountMapper;
import com.woorifisan.bank.domain.account.mapper.TransactionLedgerMapper;
import com.woorifisan.bank.domain.account.model.Account;
import com.woorifisan.bank.domain.account.model.TransactionLedger;
import com.woorifisan.bank.domain.customer.service.CustomerService;
import com.woorifisan.bank.global.exception.BusinessException;
import com.woorifisan.bank.global.response.ErrorCode;
import com.woorifisan.bank.global.security.service.SecurityService;
import com.woorifisan.bank.global.util.CryptoUtil;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 현금 출금 서비스 (E2EE 적용)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WithdrawalService {

    private final AccountMapper accountMapper;
    private final CustomerService customerService;
    private final TransactionLedgerMapper transactionLedgerMapper;
    private final PasswordEncoder passwordEncoder;
    private final SecurityService securityService;
    private final CryptoUtil cryptoUtil;

    @Transactional
    public WithdrawalResponse withdraw(WithdrawalRequest request) {
        log.info("현금 출금 요청 수신 - 키ID: {}", request.getBankKeyId());

        // 1. E2EE 복호화 및 세션키(CEK) 추출
        SecurityService.DecryptionResult<DecryptedWithdrawData> decryptionResult = 
                securityService.decryptWithKey(request, DecryptedWithdrawData.class);
        
        DecryptedWithdrawData decryptedData = decryptionResult.getData();

        // 2. 계좌 및 고객 정보 검증 (Blind Index 활용)
        Account account = accountMapper.findByAccountNoHash(cryptoUtil.hash(decryptedData.getWithdrawalAccountNo()))
                .orElseThrow(() -> new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND));

        customerService.verifyCustomerIdentification(account.getCustomerId(), decryptedData.getCustomerRrnPrefix(), decryptedData.getCustomerName());

        // 3. 출금 가능 여부 1차 체크 (비밀번호, 계좌 상태, 잔액)
        validateWithdrawal(account, decryptedData.getWithdrawalPassword(), request.getAmount());

        // 4. 비관적 락(FOR UPDATE) 획득 후 재검증
        // 동일 계좌에 대한 동시 출금 요청 시 Race Condition으로 인한 마이너스 잔액 발생을 방지합니다.
        account = accountMapper.findByIdForUpdate(account.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND));
        
        if (!"DEPOSIT".equals(account.getAccountType())) {
            throw new BusinessException(ErrorCode.INVALID_ACCOUNT_TYPE);
        }
        if (!"NORMAL".equals(account.getStatus())) {
            throw new BusinessException(ErrorCode.ACCOUNT_NOT_NORMAL);
        }
        if (account.getBalance().compareTo(request.getAmount()) < 0) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_BALANCE);
        }

        // 5. 잔액 업데이트
        accountMapper.updateBalance(account.getId(), request.getAmount().negate());

        // 6. 거래 내역(원장) 생성 및 저장
        String txId = "TXW-" + UUID.randomUUID().toString().replace("-", "").toUpperCase();
        BigDecimal balanceAfter = account.getBalance().subtract(request.getAmount());

        TransactionLedger ledger = TransactionLedger.of(
                txId,
                account.getId(),
                "WITHDRAW",
                request.getAmount().negate(),
                balanceAfter,
                null,
                null,
                "현금 출금",
                "SUCCESS"
        );
        transactionLedgerMapper.insert(ledger);

        // 7. 결과 데이터 암호화 (추출된 세션키 사용)
        WithdrawalResponse.SensitiveData sensitiveData = WithdrawalResponse.SensitiveData.builder()
                .balanceAfter(balanceAfter)
                .build();
        
        String resPayload = securityService.encryptResponse(sensitiveData, decryptionResult.getCek());

        log.info("출금 완료: 거래ID={}, 계좌ID={}, 금액={}, 잔액={}", txId, account.getId(), request.getAmount(), balanceAfter);

        return WithdrawalResponse.of(
                txId, 
                balanceAfter, 
                ledger.getTransactedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                resPayload
        );
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
        if (!passwordEncoder.matches(inputPassword, account.getPassword())) {
            throw new BusinessException(ErrorCode.ACCOUNT_PW_ERROR);
        }

        // 3. 잔액 확인
        if (account.getBalance().compareTo(amount) < 0) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_BALANCE);
        }
    }
}

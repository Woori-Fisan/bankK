package com.woorifisan.bank.domain.account.service;

import com.woorifisan.bank.domain.account.dto.decrypted.DecryptedWithdrawData;
import com.woorifisan.bank.domain.account.dto.request.InternalDepositRequest;
import com.woorifisan.bank.domain.account.dto.request.TransferRequest;
import com.woorifisan.bank.domain.account.dto.response.TransferResponse;
import com.woorifisan.bank.domain.account.mapper.AccountMapper;
import com.woorifisan.bank.domain.account.mapper.TransactionLedgerMapper;
import com.woorifisan.bank.domain.account.model.Account;
import com.woorifisan.bank.domain.account.model.TransactionLedger;
import com.woorifisan.bank.domain.customer.service.CustomerService;
import com.woorifisan.bank.global.exception.BusinessException;
import com.woorifisan.bank.global.response.ErrorCode;
import com.woorifisan.bank.global.security.service.SecurityService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 개별 이체 트랜잭션 처리 서비스 (Propagation.REQUIRES_NEW 전파 옵션 사용)
 * - Self-invocation 자가 호출 구조를 방지하기 위해 분리 설계되었습니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TransferTxService {

    private final AccountMapper accountMapper;
    private final CustomerService customerService;
    private final TransactionLedgerMapper transactionLedgerMapper;
    private final PasswordEncoder passwordEncoder;
    private final SecurityService securityService;

    /**
     * 출금 이체 실행 (독립 트랜잭션)
     * - 중복 복호화 방지를 위해 이미 복호화된 DecryptedWithdrawData와 cek를 직접 파라미터로 전달받습니다.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public TransferResponse withdrawTransfer(TransferRequest request, DecryptedWithdrawData decryptedData, javax.crypto.SecretKey cek) {
        log.info("출금 이체 실행 (독립 트랜잭션) - 출금은행: {}, 입금은행: {}, 금액: {}", 
                request.getWithdrawalBankCode(), request.getDepositBankCode(), request.getAmount());

        // 1. 계좌 조회 및 검증
        Account sender = accountMapper.findByAccountNoPlain(decryptedData.getWithdrawalAccountNo())
                .orElseThrow(() -> new BusinessException(ErrorCode.BANK_NOT_FOUND));
        
        customerService.verifyCustomerIdentification(sender.getCustomerId(), decryptedData.getCustomerRrnPrefix(), decryptedData.getCustomerName());
        
        if (!passwordEncoder.matches(decryptedData.getWithdrawalPassword(), sender.getPassword())) {
            throw new BusinessException(ErrorCode.BANK_PW_ERROR);
        }

        if (!"DEPOSIT".equals(sender.getAccountType())) {
            throw new BusinessException(ErrorCode.INVALID_ACCOUNT_TYPE);
        }

        if (!"NORMAL".equals(sender.getStatus())) {
            throw new BusinessException(ErrorCode.ACCOUNT_NOT_NORMAL);
        }

        if (sender.getBalance().compareTo(request.getAmount()) < 0) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_BALANCE);
        }

        // 2. 잔액 업데이트 (비관적 락 적용을 위해 다시 조회)
        sender = accountMapper.findByIdForUpdate(sender.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.BANK_NOT_FOUND));
        
        if (!"DEPOSIT".equals(sender.getAccountType())) {
            throw new BusinessException(ErrorCode.INVALID_ACCOUNT_TYPE);
        }
        
        if (!"NORMAL".equals(sender.getStatus())) {
            throw new BusinessException(ErrorCode.ACCOUNT_NOT_NORMAL);
        }
        
        if (sender.getBalance().compareTo(request.getAmount()) < 0) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_BALANCE);
        }

        BigDecimal newBalance = sender.getBalance().subtract(request.getAmount());
        accountMapper.updateBalance(sender.getId(), request.getAmount().negate());

        // 3. 원장 기록 (최초 상태는 PENDING)
        String txId = UUID.randomUUID().toString();
        transactionLedgerMapper.insert(TransactionLedger.of(
                txId, 
                sender.getId(), 
                "TRANSFER", 
                request.getAmount().negate(), 
                newBalance, 
                request.getDepositBankCode(), 
                decryptedData.getDepositAccountNo(), 
                "이체출금(" + request.getDepositBankCode() + "/" + decryptedData.getDepositAccountNo() + ")", 
                "PENDING"
        ));

        // 4. 결과 암호화
        TransferResponse.SensitiveData sensitiveData = TransferResponse.SensitiveData.builder()
                .balanceAfter(newBalance)
                .build();
        
        String resPayload = securityService.encryptResponse(sensitiveData, cek);

        return TransferResponse.of(txId, getCurrentTimestamp(), newBalance, resPayload);
    }

    /**
     * 이체 환불 실행 (독립 트랜잭션)
     * - 중복 복호화 방지를 위해 이미 복호화된 DecryptedWithdrawData를 직접 전달받습니다.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public TransferResponse refundTransfer(TransferRequest request, DecryptedWithdrawData originalWithdrawData) {
        return refundTransfer(request, originalWithdrawData, null);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public TransferResponse refundTransfer(TransferRequest request, DecryptedWithdrawData originalWithdrawData, String originalTxId) {
        log.info("이체 환불 실행 (독립 트랜잭션) - 원래 입금하려던 은행: {}, 금액: {}, 원 거래ID: {}", 
                request.getDepositBankCode(), request.getAmount(), originalTxId);

        // 1. 원래 출금 계좌(환불받을 계좌) 조회
        Account account = accountMapper.findByAccountNoPlain(originalWithdrawData.getWithdrawalAccountNo())
                .orElseThrow(() -> new BusinessException(ErrorCode.BANK_NOT_FOUND));
        
        account = accountMapper.findByIdForUpdate(account.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.BANK_NOT_FOUND));

        // 2. 잔액 복구 (입금)
        BigDecimal newBalance = account.getBalance().add(request.getAmount());
        accountMapper.updateBalance(account.getId(), request.getAmount());

        // 3. 원장 기록 및 원래 원장 상태 업데이트
        if (originalTxId != null) {
            transactionLedgerMapper.updateStatus(originalTxId, "FAILED");
        }

        String txId = UUID.randomUUID().toString();
        transactionLedgerMapper.insert(TransactionLedger.of(
                txId, 
                account.getId(), 
                "DEPOSIT", 
                request.getAmount(), 
                newBalance, 
                request.getDepositBankCode(), 
                originalWithdrawData.getDepositAccountNo(), 
                "이체환불(입금실패로 인한 복구)", 
                "SUCCESS"
        ));

        return TransferResponse.of(txId, getCurrentTimestamp(), newBalance);
    }

    /**
     * 내부 입금 처리 (독립 트랜잭션)
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public TransferResponse internalDeposit(InternalDepositRequest request) {
        log.info("내부 입금 처리 (독립 트랜잭션) - 출금은행: {}, 금액: {}, 트랜잭션ID: {}", 
                request.getWithdrawalBankCode(), request.getAmount(), request.getTxId());

        return depositInternal(request.getDepositAccountNo(), request.getAmount(), 
                request.getWithdrawalBankCode(), request.getWithdrawalAccountNo(), request.getTxId());
    }

    /**
     * 거래 원장의 상태 업데이트 (독립 트랜잭션)
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateLedgerStatus(String txId, String status) {
        log.info("원장 상태 업데이트 실행 (독립 트랜잭션) - 거래ID: {}, 상태: {}", txId, status);
        transactionLedgerMapper.updateStatus(txId, status);
    }

    /**
     * 공통 입금 로직 (내부용)
     */
    private TransferResponse depositInternal(String depositAccountNo, BigDecimal amount, 
                                            String withdrawBankCode, String withdrawAccountNo, String txId) {
        Account receiver = accountMapper.findByAccountNoPlain(depositAccountNo)
                .orElseThrow(() -> new BusinessException(ErrorCode.BANK_NOT_FOUND));
        
        if (!"NORMAL".equals(receiver.getStatus())) {
            throw new BusinessException(ErrorCode.ACCOUNT_NOT_NORMAL);
        }

        receiver = accountMapper.findByIdForUpdate(receiver.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.BANK_NOT_FOUND));
        
        if (!"NORMAL".equals(receiver.getStatus())) {
            throw new BusinessException(ErrorCode.ACCOUNT_NOT_NORMAL);
        }

        BigDecimal newBalance = receiver.getBalance().add(amount);
        accountMapper.updateBalance(receiver.getId(), amount);

        String effectiveTxId = (txId != null && !txId.trim().isEmpty()) ? txId : UUID.randomUUID().toString();
        transactionLedgerMapper.insert(TransactionLedger.of(
                effectiveTxId, 
                receiver.getId(), 
                "TRANSFER",
                amount, 
                newBalance, 
                withdrawBankCode, 
                withdrawAccountNo, 
                "이체입금(통합/" + withdrawBankCode + "/" + withdrawAccountNo + ")", 
                "SUCCESS"
        ));

        return TransferResponse.of(effectiveTxId, getCurrentTimestamp(), newBalance);
    }

    private String getCurrentTimestamp() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
}

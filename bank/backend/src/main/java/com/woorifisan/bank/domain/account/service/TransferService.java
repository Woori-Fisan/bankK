package com.woorifisan.bank.domain.account.service;

import com.woorifisan.bank.domain.account.dto.decrtypted.DecryptedRecipientData;
import com.woorifisan.bank.domain.account.dto.request.DepositRequest;
import com.woorifisan.bank.domain.account.dto.request.RecipientRequest;
import com.woorifisan.bank.domain.account.dto.request.TransferRequest;
import com.woorifisan.bank.domain.account.dto.response.RecipientResponse;
import com.woorifisan.bank.domain.account.dto.response.TransferResponse;
import com.woorifisan.bank.domain.account.mapper.AccountMapper;
import com.woorifisan.bank.domain.account.mapper.TransactionLedgerMapper;
import com.woorifisan.bank.domain.account.model.Account;
import com.woorifisan.bank.domain.account.model.TransactionLedger;
import com.woorifisan.bank.domain.customer.mapper.CustomerMapper;
import com.woorifisan.bank.domain.customer.model.Customer;
import com.woorifisan.bank.global.response.ErrorCode;
import com.woorifisan.bank.global.exception.BusinessException;
import com.woorifisan.bank.global.security.service.SecurityService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 이체 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TransferService {

    private final AccountMapper accountMapper;
    private final CustomerMapper customerMapper;
    private final TransactionLedgerMapper transactionLedgerMapper;
    private final PasswordEncoder passwordEncoder;
    private final SecurityService securityService;

    private static final String CURRENT_BANK_CODE = "020"; // 우리은행 코드 임시 정의

    /**
     * 이체 실행 (기존 로직 유지)
     */
    @Transactional
    public TransferResponse executeTransfer(TransferRequest request) {
        // ... (기존 로직 동일)
        if (!CURRENT_BANK_CODE.equals(request.getDepositBankCode())) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
        Account sender = accountMapper.findByAccountNoPlain(request.getWithdrawalAccountNo())
                .orElseThrow(() -> new BusinessException(ErrorCode.BANK_NOT_FOUND));
        verifyCustomerIdentification(sender.getCustomerId(), request.getCustomerRrnPrefix());
        if (!passwordEncoder.matches(request.getWithdrawalPassword(), sender.getPassword())) {
            throw new BusinessException(ErrorCode.BANK_PW_ERROR);
        }
        Account receiver = accountMapper.findByAccountNoPlain(request.getDepositAccountNo())
                .orElseThrow(() -> new BusinessException(ErrorCode.BANK_NOT_FOUND));
        if (sender.getId() < receiver.getId()) {
            sender = accountMapper.findByIdForUpdate(sender.getId()).orElseThrow(() -> new BusinessException(ErrorCode.BANK_NOT_FOUND));
            receiver = accountMapper.findByIdForUpdate(receiver.getId()).orElseThrow(() -> new BusinessException(ErrorCode.BANK_NOT_FOUND));
        } else {
            receiver = accountMapper.findByIdForUpdate(receiver.getId()).orElseThrow(() -> new BusinessException(ErrorCode.BANK_NOT_FOUND));
            sender = accountMapper.findByIdForUpdate(sender.getId()).orElseThrow(() -> new BusinessException(ErrorCode.BANK_NOT_FOUND));
        }
        if (sender.getBalance().compareTo(request.getAmount()) < 0) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_BALANCE);
        }
        BigDecimal senderNewBalance = sender.getBalance().subtract(request.getAmount());
        BigDecimal receiverNewBalance = receiver.getBalance().add(request.getAmount());
        accountMapper.updateBalance(sender.getId(), request.getAmount().negate());
        accountMapper.updateBalance(receiver.getId(), request.getAmount());
        String txId = UUID.randomUUID().toString();
        transactionLedgerMapper.insert(TransactionLedger.of(txId + "-W", sender.getId(), "TRANSFER", request.getAmount().negate(), senderNewBalance, CURRENT_BANK_CODE, request.getDepositAccountNo(), "이체출금(" + request.getDepositAccountNo() + ")", "SUCCESS"));
        transactionLedgerMapper.insert(TransactionLedger.of(txId + "-D", receiver.getId(), "TRANSFER", request.getAmount(), receiverNewBalance, CURRENT_BANK_CODE, request.getWithdrawalAccountNo(), "이체입금(" + request.getWithdrawalAccountNo() + ")", "SUCCESS"));
        return TransferResponse.of(txId, LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")), senderNewBalance);
    }

    /**
     * 수취인 확인
     * @param request 수취인 확인 요청 정보 (SecureRequest 상속)
     * @return 수취인 정보 (부분 암호화 적용)
     */
    public RecipientResponse verifyRecipient(RecipientRequest request) {
        log.info("수취인 확인 요청 수신 - 은행코드: {}, 키ID: {}", request.getDepositBankCode(), request.getBankKeyId());

        // 0. 당행 요청 여부 확인
        if (!CURRENT_BANK_CODE.equals(request.getDepositBankCode())) {
            log.warn("타행 수취인 조회 요청 거절 - 요청된 코드: {}", request.getDepositBankCode());
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }

        // 1. 공통 보안 서비스를 통해 복호화 및 CEK 추출
        SecurityService.DecryptionResult<DecryptedRecipientData> decryptionResult = 
                securityService.decryptWithKey(request, DecryptedRecipientData.class);
        
        DecryptedRecipientData decryptedData = decryptionResult.getData();

        // 2. 계좌 조회 (복호화된 계좌번호 사용)
        Account account = accountMapper.findByAccountNoPlain(decryptedData.getDepositAccountNo())
                .orElseThrow(() -> new BusinessException(ErrorCode.BANK_NOT_FOUND));

        // 3. 고객 조회
        Customer customer = customerMapper.findById(account.getCustomerId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 4. 민감 데이터(성명, 계좌번호) 암호화
        RecipientResponse.SensitiveData sensitiveData = RecipientResponse.SensitiveData.builder()
                .depositorName(customer.getCustomerName())
                .depositAccountNo(account.getAccountNo())
                .build();
        
        String resPayload = securityService.encryptResponse(sensitiveData, decryptionResult.getCek());

        // 5. 응답 생성 (민감 정보는 resPayload에, 나머지는 평문)
        return RecipientResponse.of(
                resPayload,
                "우리은행",
                account.getStatus()
        );
    }

    /**
     * 출금 이체 실행 (기존 로직 유지)
     */
    @Transactional
    public TransferResponse withdrawTransfer(TransferRequest request) {
        Account sender = accountMapper.findByAccountNoPlain(request.getWithdrawalAccountNo()).orElseThrow(() -> new BusinessException(ErrorCode.BANK_NOT_FOUND));
        verifyCustomerIdentification(sender.getCustomerId(), request.getCustomerRrnPrefix());
        if (!passwordEncoder.matches(request.getWithdrawalPassword(), sender.getPassword())) {
            throw new BusinessException(ErrorCode.BANK_PW_ERROR);
        }
        if (sender.getBalance().compareTo(request.getAmount()) < 0) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_BALANCE);
        }
        sender = accountMapper.findByIdForUpdate(sender.getId()).orElseThrow(() -> new BusinessException(ErrorCode.BANK_NOT_FOUND));
        if (sender.getBalance().compareTo(request.getAmount()) < 0) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_BALANCE);
        }
        BigDecimal newBalance = sender.getBalance().subtract(request.getAmount());
        accountMapper.updateBalance(sender.getId(), request.getAmount().negate());
        String txId = UUID.randomUUID().toString();
        transactionLedgerMapper.insert(TransactionLedger.of(txId, sender.getId(), "TRANSFER", request.getAmount().negate(), newBalance, request.getDepositBankCode(), request.getDepositAccountNo(), "타행이체출금(" + request.getDepositBankCode() + "/" + request.getDepositAccountNo() + ")", "SUCCESS"));
        return TransferResponse.of(txId, getCurrentTimestamp(), newBalance);
    }

    /**
     * 입금 이체 실행 (기존 로직 유지)
     */
    @Transactional
    public TransferResponse depositTransfer(DepositRequest request) {
        Account receiver = accountMapper.findByAccountNoPlain(request.getDepositAccountNo()).orElseThrow(() -> new BusinessException(ErrorCode.BANK_NOT_FOUND));
        receiver = accountMapper.findByIdForUpdate(receiver.getId()).orElseThrow(() -> new BusinessException(ErrorCode.BANK_NOT_FOUND));
        BigDecimal newBalance = receiver.getBalance().add(request.getAmount());
        accountMapper.updateBalance(receiver.getId(), request.getAmount());
        String txId = UUID.randomUUID().toString();
        transactionLedgerMapper.insert(TransactionLedger.of(txId, receiver.getId(), "TRANSFER", request.getAmount(), newBalance, request.getWithdrawalBankCode(), request.getWithdrawalAccountNo(), "타행이체입금(" + request.getWithdrawalBankCode() + "/" + request.getWithdrawalAccountNo() + ")", "SUCCESS"));
        return TransferResponse.of(txId, getCurrentTimestamp(), newBalance);
    }

    private void verifyCustomerIdentification(Long customerId, String requestRrnPrefix) {
        Customer customer = customerMapper.findById(customerId).orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        String rrnPrefix = customer.getRrnPrefix();
        if (!rrnPrefix.startsWith(requestRrnPrefix)) {
            throw new BusinessException(ErrorCode.IDENTIFICATION_ERROR);
        }
    }

    private String getCurrentTimestamp() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
}

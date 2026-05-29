package com.woorifisan.bank.domain.account.service;

import com.woorifisan.bank.domain.account.dto.decrypted.DecryptedDepositData;
import com.woorifisan.bank.domain.account.dto.decrypted.DecryptedRecipientData;
import com.woorifisan.bank.domain.account.dto.decrypted.DecryptedWithdrawData;
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
import com.woorifisan.bank.global.exception.BusinessException;
import com.woorifisan.bank.global.security.service.SecurityService;
import com.woorifisan.bank.global.response.ErrorCode;
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
     * 출금 이체 실행 (E2EE 적용)
     */
    @Transactional
    public TransferResponse withdrawTransfer(TransferRequest request) {
        log.info("출금 이체 요청 수신 - 출금은행: {}, 입금은행: {}, 금액: {}", 
                request.getWithdrawalBankCode(), request.getDepositBankCode(), request.getAmount());

        // 1. 복호화 및 CEK 추출
        SecurityService.DecryptionResult<DecryptedWithdrawData> decryptionResult = 
                securityService.decryptWithKey(request, DecryptedWithdrawData.class);
        
        DecryptedWithdrawData decryptedData = decryptionResult.getData();

        // 2. 계좌 조회 및 검증
        Account sender = accountMapper.findByAccountNoPlain(decryptedData.getWithdrawalAccountNo())
                .orElseThrow(() -> new BusinessException(ErrorCode.BANK_NOT_FOUND));
        
        verifyCustomerIdentification(sender.getCustomerId(), decryptedData.getCustomerRrnPrefix());
        
        if (!passwordEncoder.matches(decryptedData.getWithdrawalPassword(), sender.getPassword())) {
            throw new BusinessException(ErrorCode.BANK_PW_ERROR);
        }

        if (sender.getBalance().compareTo(request.getAmount()) < 0) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_BALANCE);
        }

        // 3. 잔액 업데이트 (비관적 락 적용을 위해 다시 조회)
        sender = accountMapper.findByIdForUpdate(sender.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.BANK_NOT_FOUND));
        
        if (sender.getBalance().compareTo(request.getAmount()) < 0) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_BALANCE);
        }

        BigDecimal newBalance = sender.getBalance().subtract(request.getAmount());
        int updatedCount = accountMapper.updateBalance(sender.getId(), request.getAmount().negate(), sender.getVersion());
        if (updatedCount == 0) {
            throw new BusinessException(ErrorCode.CONCURRENT_MODIFICATION);
        }

        // 4. 원장 기록
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
                "SUCCESS"
        ));

        // 5. 결과 암호화
        TransferResponse.SensitiveData sensitiveData = TransferResponse.SensitiveData.builder()
                .balanceAfter(newBalance)
                .build();
        
        String resPayload = securityService.encryptResponse(sensitiveData, decryptionResult.getCek());

        return TransferResponse.of(txId, getCurrentTimestamp(), newBalance, resPayload);
    }

    /**
     * 입금 이체 실행 (E2EE 적용)
     */
    @Transactional
    public TransferResponse depositTransfer(DepositRequest request) {
        log.info("입금 이체 요청 수신 - 출금은행: {}, 금액: {}", request.getWithdrawalBankCode(), request.getAmount());

        // 1. 복호화 및 CEK 추출
        SecurityService.DecryptionResult<DecryptedDepositData> decryptionResult = 
                securityService.decryptWithKey(request, DecryptedDepositData.class);
        
        DecryptedDepositData decryptedData = decryptionResult.getData();

        // 2. 계좌 조회
        Account receiver = accountMapper.findByAccountNoPlain(decryptedData.getDepositAccountNo())
                .orElseThrow(() -> new BusinessException(ErrorCode.BANK_NOT_FOUND));
        
        receiver = accountMapper.findByIdForUpdate(receiver.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.BANK_NOT_FOUND));

        // 3. 잔액 업데이트
        BigDecimal newBalance = receiver.getBalance().add(request.getAmount());
        int updatedCount = accountMapper.updateBalance(receiver.getId(), request.getAmount(), receiver.getVersion());
        if (updatedCount == 0) {
            throw new BusinessException(ErrorCode.CONCURRENT_MODIFICATION);
        }

        // 4. 원장 기록
        String txId = UUID.randomUUID().toString();
        transactionLedgerMapper.insert(TransactionLedger.of(
                txId, 
                receiver.getId(), 
                "TRANSFER",
                request.getAmount(), 
                newBalance, 
                request.getWithdrawalBankCode(), 
                decryptedData.getWithdrawalAccountNo(), 
                "이체입금(" + request.getWithdrawalBankCode() + "/" + decryptedData.getWithdrawalAccountNo() + ")", 
                "SUCCESS"
        ));

        // 5. 결과 암호화
        TransferResponse.SensitiveData sensitiveData = TransferResponse.SensitiveData.builder()
                .balanceAfter(newBalance)
                .build();
        
        String resPayload = securityService.encryptResponse(sensitiveData, decryptionResult.getCek());

        return TransferResponse.of(txId, getCurrentTimestamp(), newBalance, resPayload);
    }

    /**
     * 이체 환불 실행 (E2EE 적용)
     * - 출금 시 사용했던 페이로드를 재사용하여 원래 출금 계좌로 자금을 복구합니다.
     */
    @Transactional
    public TransferResponse refundTransfer(TransferRequest request) {
        log.info("이체 환불 요청 수신 - 원래 입금하려던 은행: {}, 금액: {}", request.getDepositBankCode(), request.getAmount());

        // 1. 복호화 (출금 시 사용된 WithdrawReqPayload를 복호화)
        SecurityService.DecryptionResult<DecryptedWithdrawData> decryptionResult = 
                securityService.decryptWithKey(request, DecryptedWithdrawData.class);
        
        DecryptedWithdrawData originalWithdrawData = decryptionResult.getData();

        // 2. 원래 출금 계좌(환불받을 계좌) 조회
        Account account = accountMapper.findByAccountNoPlain(originalWithdrawData.getWithdrawalAccountNo())
                .orElseThrow(() -> new BusinessException(ErrorCode.BANK_NOT_FOUND));
        
        account = accountMapper.findByIdForUpdate(account.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.BANK_NOT_FOUND));

        // 3. 잔액 복구 (입금)
        BigDecimal newBalance = account.getBalance().add(request.getAmount());
        int updatedCount = accountMapper.updateBalance(account.getId(), request.getAmount(), account.getVersion());
        if (updatedCount == 0) {
            throw new BusinessException(ErrorCode.CONCURRENT_MODIFICATION);
        }

        // 4. 원장 기록
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

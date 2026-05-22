package com.woorifisan.bank.domain.account.service;

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
import com.woorifisan.bank.global.util.CryptoUtil;
import com.woorifisan.bank.global.response.ErrorCode;
import com.woorifisan.bank.global.exception.BusinessException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 이체 서비스
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TransferService {

    private final CryptoUtil cryptoUtil;
    private final AccountMapper accountMapper;
    private final CustomerMapper customerMapper;
    private final TransactionLedgerMapper transactionLedgerMapper;
    private final PasswordEncoder passwordEncoder;

    private static final String CURRENT_BANK_CODE = "040"; // 우리은행 코드 임시 정의

    /**
     * 이체 실행
     * @param request 이체 요청 정보
     * @return 이체 결과
     */
    @Transactional
    public TransferResponse executeTransfer(TransferRequest request) {
        
        // 1. 당행/타행 여부 확인 (당행 이체 전용으로 한정)
        if (!CURRENT_BANK_CODE.equals(request.getDepositBankCode())) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }

        // --- 당행 이체 로직 시작 ---

        // 2. 출금 계좌 조회 및 락 (Blind Index 활용)
        String withdrawalHash = cryptoUtil.hash(request.getWithdrawalAccountNo());
        Account sender = accountMapper.findByAccountNoHashWithLock(withdrawalHash)
                .orElseThrow(() -> new BusinessException(ErrorCode.BANK_NOT_FOUND));

        // 3. 본인 인증 검증 (주민번호 앞자리)
        verifyCustomerIdentification(sender.getCustomerId(), request.getCustomerRrnPrefix());

        // 4. 비밀번호 검증 (bcrypt)
        if (!passwordEncoder.matches(request.getWithdrawalPassword(), sender.getPasswordHash())) {
            throw new BusinessException(ErrorCode.BANK_PW_ERROR);
        }

        // 5. 잔액 검증
        if (sender.getBalance().compareTo(request.getAmount()) < 0) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_BALANCE);
        }

        // 6. 입금 계좌 조회 및 락
        String depositHash = cryptoUtil.hash(request.getDepositAccountNo());
        Account receiver = accountMapper.findByAccountNoHashWithLock(depositHash)
                .orElseThrow(() -> new BusinessException(ErrorCode.BANK_NOT_FOUND));

        // 7. 잔액 업데이트 (출금/입금)
        BigDecimal senderNewBalance = sender.getBalance().subtract(request.getAmount());
        BigDecimal receiverNewBalance = receiver.getBalance().add(request.getAmount());

        accountMapper.updateBalance(sender.getId(), senderNewBalance);
        accountMapper.updateBalance(receiver.getId(), receiverNewBalance);

        // 8. 거래 원장 생성 및 저장 (출금/입금 양방향)
        String txId = UUID.randomUUID().toString();
        String encryptedWithdrawalNo = cryptoUtil.encrypt(request.getWithdrawalAccountNo());
        String encryptedDepositNo = cryptoUtil.encrypt(request.getDepositAccountNo());

        // 출금 원장 (보내는 이)
        transactionLedgerMapper.insertLedger(TransactionLedger.of(
                txId + "-W",
                sender.getId(),
                "TRANSFER",
                request.getAmount().negate(),
                senderNewBalance,
                CURRENT_BANK_CODE,
                encryptedDepositNo,
                "이체출금(" + request.getDepositAccountNo() + ")",
                "SUCCESS"
        ));

        // 입금 원장 (받는 이)
        transactionLedgerMapper.insertLedger(TransactionLedger.of(
                txId + "-D",
                receiver.getId(),
                "TRANSFER",
                request.getAmount(),
                receiverNewBalance,
                CURRENT_BANK_CODE,
                encryptedWithdrawalNo,
                "이체입금(" + request.getWithdrawalAccountNo() + ")",
                "SUCCESS"
        ));

        // 9. 응답 반환
        return TransferResponse.of(
                txId,
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                senderNewBalance
        );
    }

    /**
     * 수취인 확인
     * @param request 수취인 확인 요청 정보
     * @return 수취인 정보
     */
    public RecipientResponse verifyRecipient(RecipientRequest request) {
        // 1. 계좌번호 해싱 (Blind Index)
        String accountHash = cryptoUtil.hash(request.getDepositAccountNo());

        // 2. DB 조회
        RecipientResponse response = accountMapper.findRecipientByHash(accountHash)
                .orElseThrow(() -> new BusinessException(ErrorCode.BANK_NOT_FOUND)); // 적절한 에러 코드가 없으면 BANK_NOT_FOUND 사용

        // 3. 응답의 계좌번호를 암호문 대신 평문으로 교체 (선택 사항)
        return RecipientResponse.of(
                response.getDepositorName(),
                response.getDepositBankName(),
                request.getDepositAccountNo(), // 요청받은 평문 계좌번호 반환
                response.getAccountStatus()
        );
    }

    /**
     * 출금 이체 실행 (당행 계좌 -> 타행)
     * 타행 이체 시나리오 중 '출금' 단계만 처리합니다.
     */
    @Transactional
    public TransferResponse withdrawTransfer(TransferRequest request) {
        // 1. 출금 계좌 조회 및 락
        String withdrawalHash = cryptoUtil.hash(request.getWithdrawalAccountNo());
        Account sender = accountMapper.findByAccountNoHashWithLock(withdrawalHash)
                .orElseThrow(() -> new BusinessException(ErrorCode.BANK_NOT_FOUND));

        // 2. 본인 인증 검증 (주민번호 앞자리)
        verifyCustomerIdentification(sender.getCustomerId(), request.getCustomerRrnPrefix());

        // 3. 비밀번호 검증
        if (!passwordEncoder.matches(request.getWithdrawalPassword(), sender.getPasswordHash())) {
            throw new BusinessException(ErrorCode.BANK_PW_ERROR);
        }

        // 4. 잔액 검증
        if (sender.getBalance().compareTo(request.getAmount()) < 0) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_BALANCE);
        }

        // 5. 잔액 차감 및 업데이트
        BigDecimal newBalance = sender.getBalance().subtract(request.getAmount());
        accountMapper.updateBalance(sender.getId(), newBalance);

        // 6. 원장 기록 (출금 정보만 기록)
        String txId = UUID.randomUUID().toString();
        String encryptedTargetAccount = cryptoUtil.encrypt(request.getDepositAccountNo());

        transactionLedgerMapper.insertLedger(TransactionLedger.of(
                txId,
                sender.getId(),
                "TRANSFER",
                request.getAmount().negate(),
                newBalance,
                request.getDepositBankCode(),
                encryptedTargetAccount,
                "타행이체출금(" + request.getDepositBankCode() + "/" + request.getDepositAccountNo() + ")",
                "SUCCESS"
        ));

        return TransferResponse.of(txId, getCurrentTimestamp(), newBalance);
    }

    /**
     * 입금 이체 실행 (타행 -> 당행 계좌)
     * 타행 이체 시나리오 중 '입금' 단계만 처리합니다.
     */
    @Transactional
    public TransferResponse depositTransfer(DepositRequest request) {
        // 1. 입금 대상 계좌 조회 및 락
        String depositHash = cryptoUtil.hash(request.getDepositAccountNo());
        Account receiver = accountMapper.findByAccountNoHashWithLock(depositHash)
                .orElseThrow(() -> new BusinessException(ErrorCode.BANK_NOT_FOUND));

        // 2. 잔액 증액 및 업데이트
        BigDecimal newBalance = receiver.getBalance().add(request.getAmount());
        accountMapper.updateBalance(receiver.getId(), newBalance);

        // 3. 원장 기록 (입금 정보만 기록)
        String txId = UUID.randomUUID().toString();
        String encryptedTargetAccount = cryptoUtil.encrypt(request.getWithdrawalAccountNo());

        transactionLedgerMapper.insertLedger(TransactionLedger.of(
                txId,
                receiver.getId(),
                "TRANSFER",
                request.getAmount(),
                newBalance,
                request.getWithdrawalBankCode(),
                encryptedTargetAccount,
                "타행이체입금(" + request.getWithdrawalBankCode() + "/" + request.getWithdrawalAccountNo() + ")",
                "SUCCESS"
        ));

        return TransferResponse.of(txId, getCurrentTimestamp(), newBalance);
    }

    /**
     * 본인 인증 정보 검증 (주민번호 앞자리)
     */
    private void verifyCustomerIdentification(Long customerId, String requestRrnPrefix) {
        Customer customer = customerMapper.findById(customerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        String decryptedRrnPrefix = cryptoUtil.decrypt(customer.getRrnPrefixEnc());

        if (!decryptedRrnPrefix.startsWith(requestRrnPrefix)) {
            throw new BusinessException(ErrorCode.IDENTIFICATION_ERROR);
        }
    }

    private String getCurrentTimestamp() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
}

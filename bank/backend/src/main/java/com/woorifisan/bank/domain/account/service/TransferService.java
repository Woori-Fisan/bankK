package com.woorifisan.bank.domain.account.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import com.woorifisan.bank.domain.key.service.BankRsaKeyService;
import com.woorifisan.bank.global.response.ErrorCode;
import com.woorifisan.bank.global.exception.BusinessException;
import com.woorifisan.bank.global.util.CryptoUtil;
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
    private final CryptoUtil cryptoUtil;
    private final BankRsaKeyService bankRsaKeyService;

    private static final String CURRENT_BANK_CODE = "020"; // 우리은행 코드 임시 정의

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

        // 2. 출금 계좌 조회
        Account sender = accountMapper.findByAccountNoPlain(request.getWithdrawalAccountNo())
                .orElseThrow(() -> new BusinessException(ErrorCode.BANK_NOT_FOUND));

        // 3. 본인 인증 검증 (주민번호 앞자리)
        verifyCustomerIdentification(sender.getCustomerId(), request.getCustomerRrnPrefix());

        // 4. 비밀번호 검증 (bcrypt)
        if (!passwordEncoder.matches(request.getWithdrawalPassword(), sender.getPassword())) {
            throw new BusinessException(ErrorCode.BANK_PW_ERROR);
        }

        Account receiver = accountMapper.findByAccountNoPlain(request.getDepositAccountNo())
                .orElseThrow(() -> new BusinessException(ErrorCode.BANK_NOT_FOUND));

        // 7. 계좌 락 (ID 순서대로 락을 걸어 데드락 방지)
        if (sender.getId() < receiver.getId()) {
            sender = accountMapper.findByIdForUpdate(sender.getId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.BANK_NOT_FOUND));
            receiver = accountMapper.findByIdForUpdate(receiver.getId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.BANK_NOT_FOUND));
        } else {
            receiver = accountMapper.findByIdForUpdate(receiver.getId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.BANK_NOT_FOUND));
            sender = accountMapper.findByIdForUpdate(sender.getId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.BANK_NOT_FOUND));
        }

        // 8. 잔액 재검증 (락 획득 후 최신 상태에서 다시 확인)
        if (sender.getBalance().compareTo(request.getAmount()) < 0) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_BALANCE);
        }

        // 9. 잔액 업데이트 (출금/입금 - 매퍼의 가산 방식에 맞춰 차액만 전달)
        BigDecimal senderNewBalance = sender.getBalance().subtract(request.getAmount());
        BigDecimal receiverNewBalance = receiver.getBalance().add(request.getAmount());

        // 매퍼가 balance = balance + #{amount} 형식이므로 출금은 음수를, 입금은 양수를 전달
        accountMapper.updateBalance(sender.getId(), request.getAmount().negate());
        accountMapper.updateBalance(receiver.getId(), request.getAmount());

        // 9. 거래 원장 생성 및 저장 (출금/입금 양방향)
        String txId = UUID.randomUUID().toString();

        // 출금 원장 (보내는 이)
        transactionLedgerMapper.insert(TransactionLedger.of(
                txId + "-W",
                sender.getId(),
                "TRANSFER",
                request.getAmount().negate(),
                senderNewBalance,
                CURRENT_BANK_CODE,
                request.getDepositAccountNo(),
                "이체출금(" + request.getDepositAccountNo() + ")",
                "SUCCESS"
        ));

        // 입금 원장 (받는 이)
        transactionLedgerMapper.insert(TransactionLedger.of(
                txId + "-D",
                receiver.getId(),
                "TRANSFER",
                request.getAmount(),
                receiverNewBalance,
                CURRENT_BANK_CODE,
                request.getWithdrawalAccountNo(),
                "이체입금(" + request.getWithdrawalAccountNo() + ")",
                "SUCCESS"
        ));

        // 10. 응답 반환
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
        log.info("수취인 확인 요청 수신 - 은행코드: {}, 키ID: {}", request.getDepositBankCode(), request.getBankKeyId());

        // 1. JWE 복호화 (RSA-OAEP-256)
        // DTO에 포함된 bankKeyId를 사용하여 해당 버전의 개인키를 조회
        String privateKeyPem = bankRsaKeyService.getRawPrivateKeyByKeyId(request.getBankKeyId());
        String decryptedJson = cryptoUtil.decryptJwe(request.getReqPayload(), privateKeyPem);

        // 2. 복호화된 페이로드에서 계좌번호 추출
        String accountNo;
        try {
            JsonNode payloadNode = new ObjectMapper().readTree(decryptedJson);
            accountNo = payloadNode.path("depositAccountNo").asText();
        } catch (Exception e) {
            log.error("복호화된 페이로드 파싱 실패", e);
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }

        // 3. 계좌 조회
        Account account = accountMapper.findByAccountNoPlain(accountNo)
                .orElseThrow(() -> new BusinessException(ErrorCode.BANK_NOT_FOUND));

        // 4. 고객 조회
        Customer customer = customerMapper.findById(account.getCustomerId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 5. 응답 생성
        return RecipientResponse.of(
                customer.getCustomerName(),
                "우리은행",
                account.getAccountNo(),
                account.getStatus()
        );
    }

    /**
     * 출금 이체 실행 (당행 계좌 -> 타행)
     * 타행 이체 시나리오 중 '출금' 단계만 처리합니다.
     */
    @Transactional
    public TransferResponse withdrawTransfer(TransferRequest request) {
        // 1. 출금 계좌 조회
        Account sender = accountMapper.findByAccountNoPlain(request.getWithdrawalAccountNo())
                .orElseThrow(() -> new BusinessException(ErrorCode.BANK_NOT_FOUND));

        // 2. 본인 인증 검증 (주민번호 앞자리)
        verifyCustomerIdentification(sender.getCustomerId(), request.getCustomerRrnPrefix());

        // 3. 비밀번호 검증
        if (!passwordEncoder.matches(request.getWithdrawalPassword(), sender.getPassword())) {
            throw new BusinessException(ErrorCode.BANK_PW_ERROR);
        }

        // 4. 잔액 검증
        if (sender.getBalance().compareTo(request.getAmount()) < 0) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_BALANCE);
        }

        // 5. 계좌 락
        sender = accountMapper.findByIdForUpdate(sender.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.BANK_NOT_FOUND));

        // 6. 잔액 재검증 (락 획득 후 최신 상태에서 다시 확인)
        if (sender.getBalance().compareTo(request.getAmount()) < 0) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_BALANCE);
        }

        // 7. 잔액 차감 및 업데이트 (매퍼의 가산 방식에 맞춰 차액만 전달)
        BigDecimal newBalance = sender.getBalance().subtract(request.getAmount());
        accountMapper.updateBalance(sender.getId(), request.getAmount().negate());

        // 7. 원장 기록 (출금 정보만 기록)
        String txId = UUID.randomUUID().toString();

        transactionLedgerMapper.insert(TransactionLedger.of(
                txId,
                sender.getId(),
                "TRANSFER",
                request.getAmount().negate(),
                newBalance,
                request.getDepositBankCode(),
                request.getDepositAccountNo(),
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
        // 1. 입금 대상 계좌 조회
        Account receiver = accountMapper.findByAccountNoPlain(request.getDepositAccountNo())
                .orElseThrow(() -> new BusinessException(ErrorCode.BANK_NOT_FOUND));

        // 2. 계좌 락
        receiver = accountMapper.findByIdForUpdate(receiver.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.BANK_NOT_FOUND));

        // 3. 잔액 증액 및 업데이트 (매퍼의 가산 방식에 맞춰 차액만 전달)
        BigDecimal newBalance = receiver.getBalance().add(request.getAmount());
        accountMapper.updateBalance(receiver.getId(), request.getAmount());

        // 4. 원장 기록 (입금 정보만 기록)
        String txId = UUID.randomUUID().toString();

        transactionLedgerMapper.insert(TransactionLedger.of(
                txId,
                receiver.getId(),
                "TRANSFER",
                request.getAmount(),
                newBalance,
                request.getWithdrawalBankCode(),
                request.getWithdrawalAccountNo(),
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

        String rrnPrefix = customer.getRrnPrefix();

        if (!rrnPrefix.startsWith(requestRrnPrefix)) {
            throw new BusinessException(ErrorCode.IDENTIFICATION_ERROR);
        }
    }

    private String getCurrentTimestamp() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
}

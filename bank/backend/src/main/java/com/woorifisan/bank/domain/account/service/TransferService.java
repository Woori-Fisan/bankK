package com.woorifisan.bank.domain.account.service;

import com.woorifisan.bank.domain.account.dto.decrypted.DecryptedDepositData;
import com.woorifisan.bank.domain.account.dto.decrypted.DecryptedRecipientData;
import com.woorifisan.bank.domain.account.dto.decrypted.DecryptedWithdrawData;
import com.woorifisan.bank.domain.account.dto.request.DepositRequest;
import com.woorifisan.bank.domain.account.dto.request.InternalDepositRequest;
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
import com.woorifisan.bank.global.config.BankNetworkConfig;
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
import org.springframework.web.client.RestTemplate;

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
    private final BankNetworkConfig bankNetworkConfig;
    private final RestTemplate restTemplate;

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

    /**
     * 통합 이체 실행 (BaaS용 단일 엔드포인트)
     * 1. 출금 은행(당행)에서 출금 처리
     * 2. 입금 은행이 당행이면 직접 입금, 타행이면 타행 API 호출
     */
    @Transactional
    public TransferResponse executeTransfer(TransferRequest request) {
        log.info("통합 이체 실행 요청 수신 - 출금은행: {}, 입금은행: {}, 금액: {}", 
                request.getWithdrawalBankCode(), request.getDepositBankCode(), request.getAmount());

        // 1. 출금 처리 (당행 계좌에서 돈이 나감)
        // 기존 withdrawTransfer 로직 재활용 (복호화 및 잔액 차감 포함)
        TransferResponse withdrawalResponse = withdrawTransfer(request);
        log.info("통합 이체 Step 1: 출금 성공 - 거래ID: {}", withdrawalResponse.getTransactionId());

        // 2. 당행/타행 여부 판단
        if (CURRENT_BANK_CODE.equals(request.getDepositBankCode())) {
            // [당행 이체] 직접 입금 처리
            log.info("통합 이체 Step 2: 당행 이체 진행");
            
            // withdrawTransfer에서 이미 복호화된 데이터가 필요하므로, 로직상 결합이 필요함
            // 여기서는 편의상 내부 입금 로직을 직접 호출하거나 캡슐화된 메서드 사용
            SecurityService.DecryptionResult<DecryptedWithdrawData> decryptionResult = 
                    securityService.decryptWithKey(request, DecryptedWithdrawData.class);
            DecryptedWithdrawData decryptedData = decryptionResult.getData();

            depositInternal(decryptedData.getDepositAccountNo(), request.getAmount(), 
                    request.getWithdrawalBankCode(), decryptedData.getWithdrawalAccountNo());
            
            return withdrawalResponse;
        } else {
            // [타행 이체] 타행 입금 API 호출
            log.info("통합 이체 Step 2: 타행 이체 진행 (타행코드: {})", request.getDepositBankCode());
            
            SecurityService.DecryptionResult<DecryptedWithdrawData> decryptionResult = 
                    securityService.decryptWithKey(request, DecryptedWithdrawData.class);
            DecryptedWithdrawData decryptedData = decryptionResult.getData();

            try {
                // 타행 입금 API 호출 (실제로는 WebClient 등으로 타행 URL 호출)
                callExternalBankDeposit(request.getDepositBankCode(), InternalDepositRequest.builder()
                        .depositAccountNo(decryptedData.getDepositAccountNo())
                        .amount(request.getAmount())
                        .withdrawalBankCode(request.getWithdrawalBankCode())
                        .withdrawalAccountNo(decryptedData.getWithdrawalAccountNo())
                        .build());
                
                return withdrawalResponse;
            } catch (Exception e) {
                log.error("타행 입금 호출 실패, 환불 처리를 시작합니다: {}", e.getMessage());
                // 보상 트랜잭션: 환불
                refundTransfer(request);
                throw new BusinessException(ErrorCode.LOAN_DEPOSIT_BANK_MISMATCH, "타행 입금 처리 중 오류가 발생하여 환불되었습니다.");
            }
        }
    }

    /**
     * 내부 입금 처리 (은행 간 통신용)
     */
    @Transactional
    public TransferResponse internalDeposit(InternalDepositRequest request) {
        log.info("내부 입금 요청 수신 - 출금은행: {}, 금액: {}", request.getWithdrawalBankCode(), request.getAmount());

        return depositInternal(request.getDepositAccountNo(), request.getAmount(), 
                request.getWithdrawalBankCode(), request.getWithdrawalAccountNo());
    }

    /**
     * 공통 입금 로직 (당행 내부용)
     */
    private TransferResponse depositInternal(String depositAccountNo, BigDecimal amount, String withdrawBankCode, String withdrawAccountNo) {
        // 1. 계좌 조회
        Account receiver = accountMapper.findByAccountNoPlain(depositAccountNo)
                .orElseThrow(() -> new BusinessException(ErrorCode.BANK_NOT_FOUND));
        
        receiver = accountMapper.findByIdForUpdate(receiver.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.BANK_NOT_FOUND));

        // 2. 잔액 업데이트
        BigDecimal newBalance = receiver.getBalance().add(amount);
        int updatedCount = accountMapper.updateBalance(receiver.getId(), amount, receiver.getVersion());
        if (updatedCount == 0) {
            throw new BusinessException(ErrorCode.CONCURRENT_MODIFICATION);
        }

        // 3. 원장 기록
        String txId = UUID.randomUUID().toString();
        transactionLedgerMapper.insert(TransactionLedger.of(
                txId, 
                receiver.getId(), 
                "TRANSFER",
                amount, 
                newBalance, 
                withdrawBankCode, 
                withdrawAccountNo, 
                "이체입금(통합/" + withdrawBankCode + "/" + withdrawAccountNo + ")", 
                "SUCCESS"
        ));

        return TransferResponse.of(txId, getCurrentTimestamp(), newBalance);
    }

    /**
     * 타행 입금 API 호출 (실제 구현)
     */
    private void callExternalBankDeposit(String targetBankCode, InternalDepositRequest internalRequest) {
        BankNetworkConfig.BankProperty bankProperty = bankNetworkConfig.getBankProperty(targetBankCode);
        if (bankProperty == null) {
            log.error("타행 네트워크 설정 정보를 찾을 수 없습니다: {}", targetBankCode);
            throw new BusinessException(ErrorCode.BANK_NOT_FOUND, "지원하지 않는 입금 은행입니다.");
        }

        String url = bankProperty.getInternalDepositUrl();
        log.info("타행 입금 API 호출 실행 - URL: {}, 대상계좌: {}", url, internalRequest.getDepositAccountNo());

        try {
            // 은행 간 통신은 정형화된 JSON을 사용하며, ApiResponse 구조를 따릅니다.
            restTemplate.postForEntity(url, internalRequest, Object.class);
            log.info("타행 입금 API 호출 성공");
        } catch (Exception e) {
            log.error("타행 입금 API 통신 중 오류 발생: {}", e.getMessage());
            throw new BusinessException(ErrorCode.BANK_NOT_FOUND, "타행 통신 중 오류가 발생했습니다.");
        }
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

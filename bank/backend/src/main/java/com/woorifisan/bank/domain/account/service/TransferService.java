package com.woorifisan.bank.domain.account.service;

import com.woorifisan.bank.domain.account.dto.decrypted.DecryptedRecipientData;
import com.woorifisan.bank.domain.account.dto.decrypted.DecryptedWithdrawData;
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
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import com.woorifisan.bank.domain.account.dto.response.TransferStatusResponse;
import org.springframework.web.client.RestTemplate;

/**
 * 통합 이체 비즈니스 흐름 제어 코디네이터 서비스 (비트랜잭션 또는 readOnly 트랜잭션 사용)
 * - Self-invocation 자가 호출 구조를 해소하고 SRP를 준수하도록 리팩토링되었습니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TransferService {

    private final AccountMapper accountMapper;
    private final CustomerMapper customerMapper;
    private final TransactionLedgerMapper transactionLedgerMapper;
    private final SecurityService securityService;
    private final BankNetworkConfig bankNetworkConfig;
    private final RestTemplate restTemplate;
    
    // 개별 트랜잭션 처리를 담당하는 서브 서비스 주입
    private final TransferTxService transferTxService;

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
     * 출금 이체 실행 (개별 트랜잭션 위임 및 복호화 처리)
     */
    public TransferResponse withdrawTransfer(TransferRequest request) {
        SecurityService.DecryptionResult<DecryptedWithdrawData> decryptionResult = 
                securityService.decryptWithKey(request, DecryptedWithdrawData.class);
        return transferTxService.withdrawTransfer(request, decryptionResult.getData(), decryptionResult.getCek());
    }

    /**
     * 이체 환불 실행 (개별 트랜잭션 위임 및 복호화 처리)
     */
    public TransferResponse refundTransfer(TransferRequest request) {
        SecurityService.DecryptionResult<DecryptedWithdrawData> decryptionResult = 
                securityService.decryptWithKey(request, DecryptedWithdrawData.class);
        return transferTxService.refundTransfer(request, decryptionResult.getData());
    }

    public TransferResponse refundTransfer(TransferRequest request, String originalTxId) {
        SecurityService.DecryptionResult<DecryptedWithdrawData> decryptionResult = 
                securityService.decryptWithKey(request, DecryptedWithdrawData.class);
        return transferTxService.refundTransfer(request, decryptionResult.getData(), originalTxId);
    }

    /**
     * 내부 입금 처리 (개별 트랜잭션 위임)
     */
    public TransferResponse internalDeposit(InternalDepositRequest request) {
        return transferTxService.internalDeposit(request);
    }

    /**
     * 거래 원장의 상태 업데이트 (개별 트랜잭션 위임)
     */
    public void updateLedgerStatus(String txId, String status) {
        transferTxService.updateLedgerStatus(txId, status);
    }

    /**
     * 통합 이체 실행 흐름 제어 (코디네이터)
     * - DB 커넥션 점유 시간을 최소화하기 위해 외부 API 호출은 비트랜잭션 구간에서 수행합니다.
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public TransferResponse executeTransfer(TransferRequest request) {
        log.info("통합 이체 실행 요청 수신 - 출금은행: {}, 입금은행: {}, 금액: {}", 
                request.getWithdrawalBankCode(), request.getDepositBankCode(), request.getAmount());

        // 0. 복호화 단 한 번만 실행하여 CPU 오버헤드 최적화
        SecurityService.DecryptionResult<DecryptedWithdrawData> decryptionResult = 
                securityService.decryptWithKey(request, DecryptedWithdrawData.class);
        DecryptedWithdrawData decryptedData = decryptionResult.getData();

        // 1. 출금 처리 (독립 트랜잭션 - PENDING 상태로 시작)
        TransferResponse withdrawalResponse = transferTxService.withdrawTransfer(request, decryptedData, decryptionResult.getCek());
        String txId = withdrawalResponse.getTransactionId();
        log.info("통합 이체 Step 1: 출금 성공 (PENDING 상태) - 거래ID: {}", txId);

        // 2. 당행/타행 여부 판단
        if (CURRENT_BANK_CODE.equals(request.getDepositBankCode())) {
            // [당행 이체] 직접 입금 처리
            log.info("통합 이체 Step 2: 당행 이체 진행");
            
            try {
                // 당행 입금은 독립 트랜잭션으로 처리
                transferTxService.internalDeposit(InternalDepositRequest.builder()
                        .depositAccountNo(decryptedData.getDepositAccountNo())
                        .amount(request.getAmount())
                        .withdrawalBankCode(request.getWithdrawalBankCode())
                        .withdrawalAccountNo(decryptedData.getWithdrawalAccountNo())
                        .txId(UUID.randomUUID().toString()) // 당행 이체는 동일 DB 내 PK 중복 방지를 위해 신규 ID 생성
                        .build());
                
                // 출금 원장의 상태를 SUCCESS로 업데이트
                transferTxService.updateLedgerStatus(txId, "SUCCESS");
                
                return withdrawalResponse;
            } catch (Exception e) {
                log.error("당행 입금 처리 중 오류 발생, 환불 처리를 시작합니다: {}", e.getMessage());
                transferTxService.refundTransfer(request, decryptedData, txId);
                throw new BusinessException(ErrorCode.LOAN_DEPOSIT_BANK_MISMATCH, "당행 입금 처리 중 오류가 발생하여 환불되었습니다.");
            }
        } else {
            // [타행 이체] 타행 입금 API 호출
            log.info("통합 이체 Step 2: 타행 이체 진행 (타행코드: {})", request.getDepositBankCode());

            try {
                // 타행 입금 API 호출 (출금 거래 ID인 txId를 공유하여 전송)
                callExternalBankDeposit(request.getDepositBankCode(), InternalDepositRequest.builder()
                        .depositAccountNo(decryptedData.getDepositAccountNo())
                        .amount(request.getAmount())
                        .withdrawalBankCode(request.getWithdrawalBankCode())
                        .withdrawalAccountNo(decryptedData.getWithdrawalAccountNo())
                        .txId(txId)
                        .build());
                
                // 입금 성공 시 출금 원장 상태를 SUCCESS로 업데이트
                transferTxService.updateLedgerStatus(txId, "SUCCESS");
                
                return withdrawalResponse;
            } catch (Exception e) {
                log.error("타행 입금 호출 중 오류 발생. 상대측 거래 상태 확인을 진행합니다: {}", e.getMessage());
                
                // 이중 지급 방지를 위해 타행에 거래 상태 조회 API 호출
                boolean isAlreadyProcessed = checkExternalTransferStatus(request.getDepositBankCode(), txId);
                
                if (isAlreadyProcessed) {
                    log.info("타행 거래 상태 조회 결과: 입금 성공 확인. 당행 거래를 성공으로 마킹합니다.");
                    transferTxService.updateLedgerStatus(txId, "SUCCESS");
                    return withdrawalResponse;
                } else {
                    log.error("타행 거래 상태 조회 결과: 미처리 확인. 환불 처리를 시작합니다.");
                    // 보상 트랜잭션: 환불
                    transferTxService.refundTransfer(request, decryptedData, txId);
                    throw new BusinessException(ErrorCode.LOAN_DEPOSIT_BANK_MISMATCH, "타행 입금 처리 중 오류가 발생하여 환불되었습니다.");
                }
            }
        }
    }

    /**
     * 거래 상태 조회
     */
    public TransferStatusResponse getTransferStatus(String txId) {
        log.info("거래 상태 조회 요청 - 거래ID: {}", txId);
        TransactionLedger ledger = transactionLedgerMapper.findByTxId(txId)
                .orElseThrow(() -> new BusinessException(ErrorCode.BANK_NOT_FOUND, "해당 거래 내역이 존재하지 않습니다."));
        
        return TransferStatusResponse.builder()
                .txId(ledger.getTxId())
                .status(ledger.getStatus())
                .message("거래가 조회되었습니다.")
                .build();
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

    /**
     * 타행 거래 상태 조회 API 호출 (이중 지급 방지용 - 1초 간격으로 최대 10회 재시도)
     */
    private boolean checkExternalTransferStatus(String targetBankCode, String txId) {
        BankNetworkConfig.BankProperty bankProperty = bankNetworkConfig.getBankProperty(targetBankCode);
        if (bankProperty == null) {
            log.error("타행 네트워크 설정 정보를 찾을 수 없습니다: {}", targetBankCode);
            throw new BusinessException(ErrorCode.BANK_NOT_FOUND, "상대 은행 설정 정보를 찾을 수 없습니다.");
        }

        String url = bankProperty.getStatusQueryUrl(txId);
        log.info("타행 거래 상태 조회 API 호출 실행 - URL: {}, 거래ID: {}", url, txId);

        int maxAttempts = 10;
        int backoffMs = 1000;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                // ApiResponse 구조로 응답을 받아와 상태를 확인합니다.
                java.util.Map<String, Object> response = restTemplate.getForObject(url, java.util.Map.class);
                if (response != null && response.get("data") != null) {
                    java.util.Map<String, Object> data = (java.util.Map<String, Object>) response.get("data");
                    String status = (String) data.get("status");
                    log.info("타행 거래 상태 조회 성공 (시도 {}/{}) - 거래ID: {}, 상태: {}", 
                            attempt, maxAttempts, txId, status);
                    
                    if ("SUCCESS".equals(status)) {
                        return true;
                    } else if ("FAILED".equals(status)) {
                        // 명확히 실패한 거래로 판명되면 즉시 재시도를 중단하고 false 반환 (환불 프로세스 유도)
                        return false;
                    }
                    // status가 PENDING이거나 다른 임시 상태인 경우 계속 재시도
                }
            } catch (HttpClientErrorException e) {
                if (e.getStatusCode().value() == 404) {
                    // 404의 경우 커밋 지연 등의 상황일 수 있으므로 재시도 진행
                    log.warn("타행에서 해당 거래ID를 찾을 수 없습니다 (404 NotFound) (시도 {}/{}) - 거래ID: {}", 
                            attempt, maxAttempts, txId);
                } else {
                    // 400, 401, 403 등 비일시적인 클라이언트 오류인 경우 즉시 예외 발생 (Fail-Fast)
                    log.error("타행 거래 상태 조회 중 해결 불가능한 4xx 클라이언트 에러 발생 - 거래ID: {}, 상태코드: {}, 에러: {}", 
                            txId, e.getStatusCode(), e.getMessage());
                    throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, 
                            "상대 은행과의 통신 실패(4xx 클라이언트 오류)로 거래 조회를 즉시 중단합니다. 상태코드: " + e.getStatusCode());
                }
            } catch (HttpServerErrorException e) {
                // 5xx 서버 에러는 일시적인 시스템 에러일 수 있으므로 재시도 진행
                log.warn("타행 서버 에러 발생 (5xx) (시도 {}/{}) - 거래ID: {}, 에러: {}", 
                        attempt, maxAttempts, txId, e.getMessage());
            } catch (ResourceAccessException e) {
                // 네트워크 타임아웃, 커넥션 장애 등은 일시적인 오류로 보고 재시도 진행
                log.warn("타행 통신 네트워크 오류 발생 (시도 {}/{}) - 거래ID: {}, 에러: {}", 
                        attempt, maxAttempts, txId, e.getMessage());
            } catch (Exception e) {
                // 기타 예외(비일시적 에러 등으로 간주) 발생 시 즉시 예외를 던져 재시도를 중단 (Fail-Fast)
                log.error("타행 거래 상태 조회 중 예기치 않은 오류 발생 - 거래ID: {}, 에러: {}", txId, e.getMessage());
                throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, 
                        "거래 상태 확인 중 예기치 않은 오류가 발생하여 조회를 즉시 중단합니다. 에러: " + e.getMessage());
            }

            // 마지막 시도가 아니면 대기 후 재시도
            if (attempt < maxAttempts) {
                try {
                    Thread.sleep(backoffMs);
                } catch (InterruptedException e) {
                    log.error("재시도 대기 중 인터럽트 발생 - 거래ID: {}", txId);
                    Thread.currentThread().interrupt();
                    throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "거래 상태 확인 중 작업이 중단되었습니다.");
                }
            }
        }
        
        // 10회 시도를 초과했거나 예외가 지속되어 상태를 확정할 수 없는 경우 (UNKNOWN)
        log.error("타행 거래 상태 조회 최종 실패 - 거래 상태 확인 불가. UNKNOWN 상태로 보류합니다. 거래ID: {}", txId);
        throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, 
                "상대 은행의 거래 처리 상태를 확정할 수 없어(UNKNOWN) 이체를 보류 상태(PENDING)로 유지합니다. 관리자 확인이 필요합니다.");
    }
}

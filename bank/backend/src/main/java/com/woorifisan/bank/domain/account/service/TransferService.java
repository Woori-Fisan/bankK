package com.woorifisan.bank.domain.account.service;

import com.woorifisan.bank.domain.account.dto.decrypted.DecryptedRecipientData;
import com.woorifisan.bank.domain.account.dto.decrypted.DecryptedWithdrawData;
import com.woorifisan.bank.domain.account.dto.request.InternalDepositRequest;
import com.woorifisan.bank.domain.account.dto.request.RecipientRequest;
import com.woorifisan.bank.domain.account.dto.request.TransferRequest;
import com.woorifisan.bank.domain.account.dto.response.RecipientResponse;
import com.woorifisan.bank.domain.account.dto.response.TransferResponse;
import com.woorifisan.bank.domain.account.dto.response.TransferStatusResponse;
import com.woorifisan.bank.domain.account.mapper.AccountMapper;
import com.woorifisan.bank.domain.account.mapper.TransactionLedgerMapper;
import com.woorifisan.bank.domain.account.model.Account;
import com.woorifisan.bank.domain.account.model.TransactionLedger;
import com.woorifisan.bank.domain.customer.mapper.CustomerMapper;
import com.woorifisan.bank.domain.customer.model.Customer;
import com.woorifisan.bank.global.exception.BusinessException;
import com.woorifisan.bank.global.response.ApiResponse;
import com.woorifisan.bank.global.response.ErrorCode;
import com.woorifisan.bank.global.security.service.SecurityService;
import com.woorifisan.bank.global.util.CryptoUtil;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.async.DeferredResult;

/**
 * 통합 이체 비즈니스 흐름 제어 코디네이터 서비스
 * - 타행 이체의 외부 API 호출 및 보상 처리는 TransferCompensationService(@Async)에 위임합니다.
 * - Saga 패턴: 각 단계를 독립된 트랜잭션으로 처리하고 실패 시 보상 트랜잭션을 실행합니다.
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
    private final CryptoUtil cryptoUtil;
    private final TransferTxService transferTxService;
    private final TransferCompensationService compensationService;

    @Value("${bank.code}")
    private String CURRENT_BANK_CODE;

    @Value("${bank.name}")
    private String CURRENT_BANK_NAME;

    /**
     * 수취인 확인
     */
    public RecipientResponse verifyRecipient(RecipientRequest request) {
        log.info("수취인 확인 요청 수신 - 은행코드: {}, 키ID: {}", request.getDepositBankCode(), request.getBankKeyId());

        // 0. 당행 요청 여부 확인
        if (!CURRENT_BANK_CODE.equals(request.getDepositBankCode())) {
            log.warn("타행 수취인 조회 요청 거절 - 요청된 코드: {}", request.getDepositBankCode());
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        // 1. 공통 보안 서비스를 통해 복호화 및 CEK 추출
        SecurityService.DecryptionResult<DecryptedRecipientData> decryptionResult = 
                securityService.decryptWithKey(request, DecryptedRecipientData.class);
        
        DecryptedRecipientData decryptedData = decryptionResult.getData();

        // 2. 계좌 조회
        Account account = accountMapper.findByAccountNoHash(cryptoUtil.hash(decryptedData.getDepositAccountNo()))
                .orElseThrow(() -> new BusinessException(ErrorCode.BANK_NOT_FOUND));

        // 3. 고객 조회
        Customer customer = customerMapper.findById(account.getCustomerId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 4. 민감 데이터(성명, 계좌번호) 암호화
        RecipientResponse.SensitiveData sensitiveData = RecipientResponse.SensitiveData.builder()
                .depositorName(customer.getCustomerName())
                .depositAccountNo(cryptoUtil.decrypt(account.getAccountNoEnc()))
                .build();
        
        String resPayload = securityService.encryptResponse(sensitiveData, decryptionResult.getCek());

        // 5. 응답 생성 (민감 정보는 resPayload에, 나머지는 평문)
        return RecipientResponse.of(
                resPayload,
                CURRENT_BANK_NAME,
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
     * 통합 이체 실행 흐름 제어 (코디네이터)
     *
     * [당행 이체] 동기 처리 후 DeferredResult에 즉시 응답을 세팅합니다.
     * [타행 이체] 출금 후 보상 처리를 @Async에 위임하고 즉시 반환합니다.
     *            Tomcat 스레드가 해방되며, 보상 완료 시 DeferredResult가 응답을 세팅합니다.
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void executeTransfer(TransferRequest request,
            DeferredResult<ApiResponse<TransferResponse>> deferredResult) {

        log.info("통합 이체 실행 요청 수신 - 출금은행: {}, 입금은행: {}, 금액: {}",
                request.getWithdrawalBankCode(), request.getDepositBankCode(), request.getAmount());

        // 0. 복호화 단 한 번만 실행하여 CPU 오버헤드 최적화
        SecurityService.DecryptionResult<DecryptedWithdrawData> decryptionResult = 
                securityService.decryptWithKey(request, DecryptedWithdrawData.class);
        DecryptedWithdrawData decryptedData = decryptionResult.getData();

        // 1. 출금 처리 (독립 트랜잭션 - PENDING 상태로 시작)
        // 출금 계좌에서 금액을 차감하고 '대기' 상태의 원장을 기록합니다.
        TransferResponse withdrawalResponse = transferTxService.withdrawTransfer(request, decryptedData, decryptionResult.getCek());
        String txId = withdrawalResponse.getTransactionId();
        log.info("통합 이체 Step 1: 출금 성공 (PENDING 상태) - 거래ID: {}", txId);

        // 2. 당행/타행 여부 판단 및 입금 처리
        if (CURRENT_BANK_CODE.equals(request.getDepositBankCode())) {
            // [당행 이체] 직접 입금 처리
            log.info("통합 이체 Step 2: 당행 이체 진행");
            handleSameBank(request, decryptedData, txId, withdrawalResponse, deferredResult);
        } else {
            log.info("통합 이체 Step 2: 타행 이체 진행 (타행코드: {})", request.getDepositBankCode());
            handleExternalBank(request, decryptedData, txId, withdrawalResponse, deferredResult);
        }
    }

    /**
     * 당행 이체 처리 (동기)
     */
    private void handleSameBank(TransferRequest request, DecryptedWithdrawData decryptedData,
            String txId, TransferResponse withdrawalResponse,
            DeferredResult<ApiResponse<TransferResponse>> deferredResult) {
        try {
            transferTxService.internalDeposit(InternalDepositRequest.builder()
                    .depositAccountNo(decryptedData.getDepositAccountNo())
                    .amount(request.getAmount())
                    .withdrawalBankCode(request.getWithdrawalBankCode())
                    .withdrawalAccountNo(decryptedData.getWithdrawalAccountNo())
                    .txId(UUID.randomUUID().toString())
                    .build());

            transferTxService.updateLedgerStatus(txId, "SUCCESS");
            deferredResult.setResult(ApiResponse.success(withdrawalResponse));

        } catch (Exception e) {
            log.error("당행 입금 처리 중 오류 발생, 환불 처리를 시작합니다 - 거래ID: {}, 오류: {}", txId, e.getMessage());
            transferTxService.refundTransfer(request, decryptedData, txId);
            deferredResult.setErrorResult(
                    new BusinessException(ErrorCode.LOAN_DEPOSIT_BANK_MISMATCH,
                            "당행 입금 처리 중 오류가 발생하여 환불되었습니다."));
        }
    }

    /**
     * 타행 이체 처리 — 보상 로직을 @Async에 위임하고 즉시 반환 (Tomcat 스레드 해방)
     */
    private void handleExternalBank(TransferRequest request, DecryptedWithdrawData decryptedData,
            String txId, TransferResponse withdrawalResponse,
            DeferredResult<ApiResponse<TransferResponse>> deferredResult) {

        InternalDepositRequest depositRequest = InternalDepositRequest.builder()
                .depositAccountNo(decryptedData.getDepositAccountNo())
                .amount(request.getAmount())
                .withdrawalBankCode(request.getWithdrawalBankCode())
                .withdrawalAccountNo(decryptedData.getWithdrawalAccountNo())
                .txId(txId)
                .build();

        // @Async — 즉시 반환, transfer-comp-N 스레드에서 실행
        compensationService.compensate(
                request.getDepositBankCode(),
                depositRequest,
                request,
                decryptedData,
                txId,
                withdrawalResponse,
                deferredResult);
    }
}
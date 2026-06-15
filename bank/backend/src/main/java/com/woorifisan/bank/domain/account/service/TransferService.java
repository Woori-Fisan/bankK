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
import com.woorifisan.bank.global.config.BankNetworkConfig;
import com.woorifisan.bank.global.exception.BusinessException;
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
    private final BankNetworkConfig bankNetworkConfig; // 타행 이체 전 입금 은행 설정 사전 검증용

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
     * [당행 이체] 동기 처리 후 SUCCESS 응답을 즉시 반환합니다.
     * [타행 이체] 출금 후 PENDING 응답을 즉시 반환하고, 보상 처리는 @Async에 위임합니다.
     *            클라이언트는 /transfer/status/{txId}로 최종 상태를 폴링합니다.
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public TransferResponse executeTransfer(TransferRequest request) {

        log.info("통합 이체 실행 요청 수신 - 출금은행: {}, 입금은행: {}, 금액: {}",
                request.getWithdrawalBankCode(), request.getDepositBankCode(), request.getAmount());

        // 0. 출금 전에 입금 은행 설정 존재 여부를 검증한다.
        if (bankNetworkConfig.getBankProperty(request.getDepositBankCode()) == null) {
            throw new BusinessException(ErrorCode.BANK_NOT_FOUND, "지원하지 않는 입금 은행입니다.");
        }

        // 1. 복호화 단 한 번만 실행하여 CPU 오버헤드 최적화
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
            return handleSameBank(request, decryptedData, txId, withdrawalResponse);
        } else {
            log.info("통합 이체 Step 2: 타행 이체 진행 (타행코드: {})", request.getDepositBankCode());
            return handleExternalBank(request, decryptedData, txId, withdrawalResponse);
        }
    }

    /**
     * 당행 이체 처리 (동기) — 입금까지 완료 후 SUCCESS 응답 반환
     */
    private TransferResponse handleSameBank(TransferRequest request, DecryptedWithdrawData decryptedData,
            String txId, TransferResponse withdrawalResponse) {
        try {
            transferTxService.internalDeposit(InternalDepositRequest.builder()
                    .depositAccountNo(decryptedData.getDepositAccountNo())
                    .amount(request.getAmount())
                    .withdrawalBankCode(request.getWithdrawalBankCode())
                    .withdrawalAccountNo(decryptedData.getWithdrawalAccountNo())
                    .txId(UUID.randomUUID().toString())
                    .build());
        } catch (Exception e) {
            // 입금 자체가 실패 → 돈이 이동하지 않았으므로 환불 시도
            log.error("당행 입금 처리 중 오류 발생, 환불 처리를 시작합니다 - 거래ID: {}, 오류: {}", txId, e.getMessage());
            try {
                transferTxService.refundTransfer(request, decryptedData, txId);
            } catch (Exception refundEx) {
                // 출금됐으나 입금·환불 모두 실패 → 관리자 확인 필요
                log.error("환불 처리 실패 - 출금액 미회수, 관리자 확인 필요 - 거래ID: {}", txId, refundEx);
                throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR,
                        "입금 실패 후 환불에도 실패했습니다. 관리자 확인이 필요합니다.");
            }
            throw new BusinessException(ErrorCode.LOAN_DEPOSIT_BANK_MISMATCH,
                    "당행 입금 처리 중 오류가 발생하여 환불되었습니다.");
        }

        try {
            transferTxService.updateLedgerStatus(txId, "SUCCESS");
        } catch (Exception e) {
            // 입금은 완료 후 원장 업데이트 실패 → 환불하면 이중 지급 발생. 관리자 확인이 필요한 예외로 처리
            log.error("원장 상태 업데이트 실패 - 입금은 완료됨, 관리자 확인 필요 - 거래ID: {}", txId, e);
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR,
                    "입금은 완료됐으나 원장 업데이트에 실패했습니다. 관리자 확인이 필요합니다.");
        }

        return withdrawalResponse;
    }

    /**
     * 타행 이체 처리 — 보상 로직을 @Async에 위임하고 PENDING 상태로 즉시 반환
     */
    private TransferResponse handleExternalBank(TransferRequest request, DecryptedWithdrawData decryptedData,
            String txId, TransferResponse withdrawalResponse) {

        InternalDepositRequest depositRequest = InternalDepositRequest.builder()
                .depositAccountNo(decryptedData.getDepositAccountNo())
                .amount(request.getAmount())
                .withdrawalBankCode(request.getWithdrawalBankCode())
                .withdrawalAccountNo(decryptedData.getWithdrawalAccountNo())
                .txId(txId)
                .build();

        compensationService.compensate(
                request.getDepositBankCode(),
                depositRequest,
                request,
                decryptedData,
                txId);

        return withdrawalResponse;
    }
}
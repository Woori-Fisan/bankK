package com.woorifisan.platform.domain.bank.service;

import com.woorifisan.platform.domain.bank.dto.request.TransferRecipientRequest;
import com.woorifisan.platform.domain.bank.dto.request.TransferRequest;
import com.woorifisan.platform.domain.bank.dto.response.TransferRecipientResponse;
import com.woorifisan.platform.domain.bank.dto.response.TransferResponse;
import com.woorifisan.platform.domain.bank.external.client.BankExternalClient;
import com.woorifisan.platform.domain.bank.external.dto.BankRecipientRequest;
import com.woorifisan.platform.domain.bank.external.dto.BankTransferResponse;
import com.woorifisan.platform.domain.bank.external.dto.BankTransferWithdrawRequest;
import com.woorifisan.platform.domain.bank.external.dto.BankDepositRequest;
import com.woorifisan.platform.global.exception.BusinessException;
import com.woorifisan.platform.global.lock.TxSessionLockService;
import com.woorifisan.platform.global.response.ErrorCode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 이체 서비스
 * - 수취인 조회 및 이체 실행 로직을 처리함
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TransferService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final BankExternalClient bankExternalClient;
    private final TxSessionLockService sessionLockService;

    /**
     * 수취인 조회 (실제 연동)
     * @param request 수취인 조회 요청 정보
     * @param bankKeyId 헤더로 전달된 은행 키 ID
     * @return 조회된 수취인 정보
     */
    @Transactional(readOnly = true)
    public TransferRecipientResponse getRecipient(TransferRecipientRequest request, String bankKeyId) {
        log.info("수취인 조회 요청 중계 - 은행코드: {}, 키ID: {}", request.getDepositBankCode(), bankKeyId);

        // [AOP로 위임] 단말기 JWS 서명 검증 및 무결성 체크는 TerminalSignatureAspect에서 수행됨

        // 1. 은행 코어에 전달할 요청 DTO 생성 (Zero-Knowledge Pass-through)
        BankRecipientRequest bankRequest = BankRecipientRequest.of(
                request.getDepositBankCode(),
                request.getReqPayload(),
                bankKeyId
        );

        // 2. 외부 클라이언트를 통해 은행 코어 API 호출 및 결과 직접 반환 (Pass-through)
        return bankExternalClient.fetchRecipient(
                request.getDepositBankCode(),
                bankRequest
        );
    }

    /**
     * 이체 실행 (통합 API 연동)
     * @param request 이체 실행 요청 정보
     * @param jwsSignature 단말기 JWS 서명
     * @param withdrawKeyId 출금 은행 키 ID
     * @param depositKeyId 입금 은행 키 ID
     * @param staffId 요청 직원 ID (세션 락 키로 사용)
     * @return 이체 처리 결과
     */
    @Transactional
    public TransferResponse executeTransfer(TransferRequest request, String jwsSignature,
            String withdrawKeyId, String depositKeyId, Long staffId) {
        log.info("이체 실행 요청 수신 (통합) - 출금은행: {}, 입금은행: {}, 금액: {}, staffId: {}",
                request.getWithdrawalBankCode(), request.getDepositBankCode(), request.getAmount(), staffId);

        sessionLockService.acquireLock(staffId);
        try {
            // 1. 통합 이체 요청 DTO 생성
            BankTransferWithdrawRequest executeRequest = BankTransferWithdrawRequest.of(
                    request.getWithdrawReqPayload(),
                    withdrawKeyId,
                    request.getWithdrawalBankCode(),
                    request.getDepositBankCode(),
                    request.getAmount()
            );

            // 2. 은행 코어 호출 직전 PROCESSING으로 전환
            sessionLockService.upgradeToProcessing(staffId);

            // 3. 출금 은행의 통합 이체 API 호출
            BankTransferResponse response = bankExternalClient.fetchTransferExecute(
                    request.getWithdrawalBankCode(),
                    executeRequest
            );

            log.info("통합 이체 성공 - 거래ID: {}", response.getTransactionId());

            // 4. 최종 응답 반환
            return TransferResponse.builder()
                    .transactionId(response.getTransactionId())
                    .transactionDate(formatTransactionDate(response.getTransactionDate()))
                    .balanceAfter(response.getBalanceAfter())
                    .resPayload(response.getResPayload())
                    .build();
        } finally {
            sessionLockService.releaseLock(staffId);
        }
    }

    /**
     * 거래 일시 포맷팅 (yyyy-MM-dd)
     */
    private String formatTransactionDate(String transactionDate) {
        if (transactionDate == null) {
            return LocalDateTime.now().format(DATE_FORMATTER);
        }
        try {
            // 은행 코어는 "yyyy-MM-dd HH:mm:ss" 포맷으로 준다고 가정
            LocalDateTime bankDate = LocalDateTime.parse(transactionDate, 
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            return bankDate.format(DATE_FORMATTER);
        } catch (Exception e) {
            log.warn("은행 응답 날짜 파싱 실패, 현재 날짜를 사용합니다: {}", e.getMessage());
            return LocalDateTime.now().format(DATE_FORMATTER);
        }
    }
}

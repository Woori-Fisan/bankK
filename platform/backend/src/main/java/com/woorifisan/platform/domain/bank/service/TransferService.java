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
     * 이체 실행
     * @param request 이체 실행 요청 정보
     * @param jwsSignature 단말기 JWS 서명
     * @param withdrawKeyId 출금 은행 키 ID
     * @param depositKeyId 입금 은행 키 ID
     * @return 이체 처리 결과
     */
    @Transactional
    public TransferResponse executeTransfer(TransferRequest request, String jwsSignature, String withdrawKeyId, String depositKeyId) {
        log.info("이체 실행 요청 수신 - 출금은행: {}, 입금은행: {}, 금액: {}", 
                request.getWithdrawalBankCode(), request.getDepositBankCode(), request.getAmount());

        // Saga 패턴의 오케스트레이션 방식을 적용하여 원자성을 보장합니다.
        // 당행/타행 구분 없이 무조건 출금 후 입금 방식으로 처리하며, E2EE Pass-through를 수행합니다.

        // 1. [Step 1: 출금] 출금 은행 API 호출
        BankTransferWithdrawRequest withdrawRequest = BankTransferWithdrawRequest.of(
                request.getWithdrawReqPayload(),
                withdrawKeyId,
                request.getWithdrawalBankCode(),
                request.getDepositBankCode(),
                request.getAmount()
        );
        
        BankTransferResponse withdrawResponse = bankExternalClient.fetchTransferWithdraw(
                request.getWithdrawalBankCode(), 
                withdrawRequest
        );
        log.info("이체 Step 1 성공 [출금 완료] - 거래ID: {}", withdrawResponse.getTransactionId());

        try {
            // 2. [Step 2: 입금] 입금 은행 API 호출
            BankDepositRequest depositRequest = BankDepositRequest.of(
                    request.getDepositReqPayload(),
                    depositKeyId,
                    request.getAmount(),
                    request.getWithdrawalBankCode()
            );

            BankTransferResponse depositResponse = bankExternalClient.fetchDeposit(
                    request.getDepositBankCode(),
                    depositRequest
            );
            log.info("이체 Step 2 성공 [입금 완료] - 거래ID: {}", depositResponse.getTransactionId());

            // 3. 최종 응답 반환 (출금 잔액 정보를 포함한 응답을 위해 withdrawResponse의 데이터를 활용할 수도 있음)
            return TransferResponse.builder()
                    .transactionId(depositResponse.getTransactionId())
                    .transactionDate(formatTransactionDate(depositResponse.getTransactionDate()))
                    .balanceAfter(withdrawResponse.getBalanceAfter())
                    .resPayload(withdrawResponse.getResPayload()) // 출금 은행의 암호화된 응답 페이로드 전달
                    .build();

        } catch (Exception e) {
            // 4. [Step 3: 보상 트랜잭션] 입금 실패 시 출금 은행으로 자금 복구(환불) 호출
            log.error("이체 Step 2 실패 [입금 에러]. 보상 트랜잭션(환불)을 시작합니다. 에러: {}", e.getMessage());
            
            try {
                // 출금 은행에 다시 입금(환불) 요청 전송
                // 플랫폼은 계좌번호를 모르지만, 출금 시 사용했던 암호화 페이로드를 다시 전달함으로써
                // 은행 코어가 내부적으로 복호화하여 원래 계좌로 환불할 수 있도록 합니다.
                BankDepositRequest refundRequest = BankDepositRequest.of(
                        request.getWithdrawReqPayload(),
                        withdrawKeyId,
                        request.getAmount(),
                        request.getDepositBankCode()
                );

                bankExternalClient.fetchRefund(request.getWithdrawalBankCode(), withdrawRequest);
                log.info("보상 트랜잭션 성공 [자금 복구 완료] - 출금 계좌로 금액이 환불되었습니다.");
            } catch (Exception refundError) {
                // 보상 트랜잭션까지 실패한 경우 (매우 위험한 상태 - 수동 개입 필요)
                log.error("!!! [심각] 보상 트랜잭션 실패 !!! 자금 불일치 발생 가능성. 수동 확인이 필요합니다. 에러: {}", refundError.getMessage());
            }

            // 원래 발생했던 예외를 다시 던져서 사용자에게 에러 알림
            if (e instanceof BusinessException) throw (BusinessException) e;
            throw new BusinessException(ErrorCode.BANK_API_ERROR, "이체 중 입금에 실패하여 환불 처리를 시도했습니다.");
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

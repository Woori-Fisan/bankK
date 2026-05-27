package com.woorifisan.platform.domain.bank.service;

import com.woorifisan.platform.domain.bank.dto.request.TransferRecipientRequest;
import com.woorifisan.platform.domain.bank.dto.request.TransferRequest;
import com.woorifisan.platform.domain.bank.dto.response.TransferRecipientResponse;
import com.woorifisan.platform.domain.bank.dto.response.TransferResponse;
import com.woorifisan.platform.domain.bank.external.client.BankExternalClient;
import com.woorifisan.platform.domain.bank.external.dto.BankRecipientRequest;
import com.woorifisan.platform.domain.bank.external.dto.BankRecipientResponse;
import com.woorifisan.platform.domain.bank.external.dto.BankTransferRequest;
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

        // 2. 외부 클라이언트를 통해 은행 코어 API 호출
        BankRecipientResponse bankResponse = bankExternalClient.fetchRecipient(
                request.getDepositBankCode(),
                bankRequest
        );

        // 3. 응답 DTO 변환 및 반환
        return TransferRecipientResponse.builder()
                .depositorName(bankResponse.getDepositorName())
                .depositBankName(bankResponse.getDepositBankName())
                .depositBankAccountNo(bankResponse.getDepositAccountNo())
                .accountStatus(bankResponse.getAccountStatus())
                .build();
    }

    /**
     * 이체 실행
     * @param request 이체 실행 요청 정보
     * @return 이체 처리 결과
     */
    @Transactional
    public TransferResponse executeTransfer(TransferRequest request) {
        log.info("이체 실행 요청 수신 - 출금은행: {}, 출금계좌: {}, 입금은행: {}, 입금계좌: {}, 금액: {}", 
                request.getWithdrawalBankCode(), request.getWithdrawalAccountNo(),
                request.getDepositBankCode(), request.getDepositAccountNo(), request.getAmount());

        // 출금 은행과 입금 은행이 같은지 확인하여 당행/타행 이체 구분
        if (request.getWithdrawalBankCode().equals(request.getDepositBankCode())) {
            return processInternalTransfer(request);
        } else {
            return processExternalTransfer(request);
        }
    }

    /**
     * 당행 이체 처리 (출금은행 == 입금은행)
     */
    private TransferResponse processInternalTransfer(TransferRequest request) {
        log.info("당행 이체 프로세스 시작 - 은행코드: {}", request.getWithdrawalBankCode());
        
        // 1. 은행 코어에 전달할 요청 DTO 생성
        BankTransferRequest bankRequest = BankTransferRequest.of(
                request.getWithdrawalAccountNo(),
                request.getWithdrawalPassword(),
                request.getCustomerRrnPrefix(),
                request.getDepositBankCode(),
                request.getDepositAccountNo(),
                request.getAmount()
        );

        // 2. 외부 클라이언트를 통해 은행 코어 API 호출
        BankTransferResponse bankResponse = bankExternalClient.executeTransfer(
                request.getWithdrawalBankCode(),
                bankRequest
        );

        // 3. 응답 DTO 변환 및 반환 (날짜 포맷 적용)
        String formattedDate = LocalDateTime.now().format(DATE_FORMATTER);
        try {
            if (bankResponse.getTransactionDate() != null) {
                // 은행 코어는 "yyyy-MM-dd HH:mm:ss" 포맷으로 준다고 가정
                LocalDateTime bankDate = LocalDateTime.parse(bankResponse.getTransactionDate(), 
                        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                formattedDate = bankDate.format(DATE_FORMATTER);
            }
        } catch (Exception e) {
            log.warn("은행 응답 날짜 파싱 실패, 현재 날짜를 사용합니다: {}", e.getMessage());
        }

        return TransferResponse.builder()
                .transactionId(bankResponse.getTransactionId())
                .transactionDate(formattedDate)
                .balanceAfter(bankResponse.getBalanceAfter())
                .build();
    }

    /**
     * 타행 이체 처리 (출금은행 != 입금은행)
     * Saga 패턴의 오케스트레이션 방식을 적용하여 원자성을 보장합니다.
     */
    private TransferResponse processExternalTransfer(TransferRequest request) {
        log.info("타행 이체 프로세스 시작 - 출금은행: {}, 입금은행: {}", 
                request.getWithdrawalBankCode(), request.getDepositBankCode());

        // 1. [Step 1: 출금] 출금 은행 API 호출
        BankTransferWithdrawRequest withdrawRequest = BankTransferWithdrawRequest.of(
                request.getEncryptedKey(),
                request.getJwsSignature(),
                request.getWithdrawalAccountNo(),
                request.getWithdrawalPassword(),
                request.getCustomerRrnPrefix(),
                request.getDepositBankCode(),
                request.getDepositAccountNo(),
                request.getAmount()
        );
        
        BankTransferResponse withdrawResponse = bankExternalClient.fetchTransferWithdraw(
                request.getWithdrawalBankCode(), 
                withdrawRequest
        );
        log.info("타행 이체 Step 1 성공 [출금 완료] - 거래ID: {}", withdrawResponse.getTransactionId());

        try {
            // 2. [Step 2: 입금] 입금 은행 API 호출
            BankDepositRequest depositRequest = BankDepositRequest.of(
                    request.getDepositAccountNo(),
                    request.getAmount(),
                    request.getWithdrawalBankCode(),
                    request.getWithdrawalAccountNo()
            );

            BankTransferResponse depositResponse = bankExternalClient.fetchDeposit(
                    request.getDepositBankCode(),
                    depositRequest
            );
            log.info("타행 이체 Step 2 성공 [입금 완료] - 거래ID: {}", depositResponse.getTransactionId());

            // 3. 최종 응답 반환
            return TransferResponse.builder()
                    .transactionId(depositResponse.getTransactionId())
                    .transactionDate(LocalDateTime.now().format(DATE_FORMATTER))
                    .balanceAfter(withdrawResponse.getBalanceAfter())
                    .build();

        } catch (Exception e) {
            // 4. [Step 3: 보상 트랜잭션] 입금 실패 시 출금 은행으로 자금 복구(환불) 호출
            log.error("타행 이체 Step 2 실패 [입금 에러]. 보상 트랜잭션(환불)을 시작합니다. 에러: {}", e.getMessage());
            
            try {
                // 출금 은행에 다시 입금(환불) 요청 전송
                BankDepositRequest refundRequest = BankDepositRequest.of(
                        request.getWithdrawalAccountNo(), // 출금했던 계좌로
                        request.getAmount(),
                        request.getDepositBankCode(),    // 원래 입금하려던 은행 정보 기재
                        request.getDepositAccountNo()
                );

                bankExternalClient.fetchDeposit(request.getWithdrawalBankCode(), refundRequest);
                log.info("보상 트랜잭션 성공 [자금 복구 완료] - 출금 계좌로 금액이 환불되었습니다.");
            } catch (Exception refundError) {
                // 보상 트랜잭션까지 실패한 경우 (매우 위험한 상태 - 수동 개입 필요)
                log.error("!!! [심각] 보상 트랜잭션 실패 !!! 자금 불일치 발생 가능성. 수동 확인이 필요합니다. 에러: {}", refundError.getMessage());
            }

            // 원래 발생했던 예외를 다시 던져서 사용자에게 에러 알림
            if (e instanceof BusinessException) throw (BusinessException) e;
            throw new BusinessException(ErrorCode.BANK_API_ERROR, "타행 이체 중 입금에 실패하여 환불 처리를 시도했습니다.");
        }
    }
}

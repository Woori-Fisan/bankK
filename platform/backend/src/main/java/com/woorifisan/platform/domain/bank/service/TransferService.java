package com.woorifisan.platform.domain.bank.service;

import com.woorifisan.platform.domain.bank.dto.request.TransferRecipientRequest;
import com.woorifisan.platform.domain.bank.dto.request.TransferRequest;
import com.woorifisan.platform.domain.bank.dto.response.TransferRecipientResponse;
import com.woorifisan.platform.domain.bank.dto.response.TransferResponse;
import com.woorifisan.platform.domain.bank.external.client.BankExternalClient;
import com.woorifisan.platform.domain.bank.external.dto.BankRecipientRequest;
import com.woorifisan.platform.domain.bank.external.dto.BankTransferResponse;
import com.woorifisan.platform.domain.bank.external.dto.BankTransferWithdrawRequest;
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

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final BankExternalClient bankExternalClient;

    /**
     * 수취인 조회 (실제 연동)
     * @param request 수취인 조회 요청 정보
     * @param bankKeyId 헤더로 전달된 은행 키 ID
     * @return 조회된 수취인 정보
     */
    @Transactional(readOnly = true)
    public TransferRecipientResponse getRecipient(TransferRecipientRequest request, String bankKeyId) {
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
     * @return 이체 처리 결과
     */
    public TransferResponse executeTransfer(TransferRequest request, String jwsSignature, String withdrawKeyId) {
        // 은행 측에서 통합 이체 로직을 처리하므로, 플랫폼은 출금 은행으로 단일 요청을 보냅니다.
        // E2EE 암호문은 출금 은행의 공개키로 암호화된 것을 사용합니다.
        BankTransferWithdrawRequest executeRequest = BankTransferWithdrawRequest.of(
                request.getReqPayload(),
                withdrawKeyId,
                request.getWithdrawalBankCode(),
                request.getDepositBankCode(),
                request.getAmount()
        );

        BankTransferResponse response = bankExternalClient.fetchTransferExecute(
                request.getWithdrawalBankCode(),
                executeRequest
        );

        // 3. 최종 응답 반환
        return TransferResponse.builder()
                .transactionId(response.getTransactionId())
                .transactionDate(formatTransactionDate(response.getTransactionDate()))
                .balanceAfter(response.getBalanceAfter())
                .resPayload(response.getResPayload())
                .build();
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

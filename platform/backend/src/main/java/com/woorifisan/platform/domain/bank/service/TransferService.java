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
import com.woorifisan.platform.global.exception.BusinessException;
import com.woorifisan.platform.global.response.ErrorCode;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
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
     * @return 조회된 수취인 정보
     */
    @Transactional(readOnly = true)
    public TransferRecipientResponse getRecipient(TransferRecipientRequest request) {
        log.info("수취인 조회 요청 수신 - 은행코드: {}, 계좌번호: {}", 
                request.getDepositBankCode(), request.getDepositAccountNo());

        // 1. 은행 코어에 전달할 요청 DTO 생성
        BankRecipientRequest bankRequest = BankRecipientRequest.of(
                request.getDepositBankCode(),
                request.getDepositAccountNo()
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
     */
    private TransferResponse processExternalTransfer(TransferRequest request) {
        log.info("타행 이체 프로세스 시작 - 출금은행: {}, 입금은행: {}", 
                request.getWithdrawalBankCode(), request.getDepositBankCode());

        // TODO: 타행 이체 오케스트레이션 로직 구현 예정
        // 1. 출금 은행 API 호출 (/transfer/withdraw)
        // 2. 입금 은행 API 호출 (/transfer/deposit)
        // 3. 실패 시 보상 트랜잭션(Rollback) 처리 고려
        
        return TransferResponse.builder()
                .transactionId("TR-EXT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .transactionDate(LocalDateTime.now().format(DATE_FORMATTER))
                .build();
    }
}

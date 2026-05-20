package com.woorifisan.platform.bank.service;

import com.woorifisan.platform.bank.dto.TransferRecipientRequest;
import com.woorifisan.platform.bank.dto.TransferRecipientResponse;
import com.woorifisan.platform.bank.dto.TransferRequest;
import com.woorifisan.platform.bank.dto.TransferResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * 이체 서비스
 * - 수취인 조회 및 이체 실행 로직을 처리함
 */
@Slf4j
@Service
public class TransferService {

    /**
     * 수취인 조회 (Mock 구현)
     * @param request 수취인 조회 요청 정보
     * @return 조회된 수취인 정보 (더미 데이터)
     */
    @Transactional(readOnly = true)
    public TransferRecipientResponse getRecipient(TransferRecipientRequest request) {
        log.info("수취인 조회 요청 수신 - 은행코드: {}, 계좌번호: {}", 
                request.getDepositBankCode(), request.getDepositAccountNo());

        // 임시 더미 데이터 생성 및 반환
        return TransferRecipientResponse.builder()
                .depositorName("홍길동")
                .depositBankName("우리어리은행")
                .depositBankAccountNo(request.getDepositAccountNo())
                .accountStatus("NORMAL")
                .build();
    }

    /**
     * 이체 실행 (Mock 구현)
     * @param request 이체 실행 요청 정보
     * @return 이체 처리 결과 (더미 데이터)
     */
    @Transactional
    public TransferResponse executeTransfer(TransferRequest request) {
        log.info("이체 실행 요청 수신 - 출금계좌: {}, 입금계좌: {}, 금액: {}", 
                request.getWithdrawalAccountNo(), request.getDepositAccountNo(), request.getAmount());

        // 임시 더미 데이터 생성 및 반환
        return TransferResponse.builder()
                .transactionId("TR-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .transactionDate(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                .balanceAfter(new BigDecimal("1000000").subtract(request.getAmount()))
                .build();
    }
}

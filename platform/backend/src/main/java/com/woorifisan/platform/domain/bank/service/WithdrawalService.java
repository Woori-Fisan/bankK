package com.woorifisan.platform.domain.bank.service;

import com.woorifisan.platform.domain.bank.dto.request.WithdrawalRequest;
import com.woorifisan.platform.domain.bank.dto.response.TransferResponse;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class WithdrawalService {

    /***
     * 출금 실행 (Mock 구현)
     * @param request 출금 요청 정보
     * @return 출금 결과 정보 (더미 데이터)
     */
    @Transactional
    public TransferResponse executeWithdraw (WithdrawalRequest request) {
        log.info("출금 실행 요청 수신 - 출금계좌: {}, 금액: {}",
                request.getWithdrawalAccountNo(), request.getAmount());

        // 임시 더미 데이터 생성 및 반환
        return TransferResponse.builder()
                .transactionId("TR-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .transactionDate(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                .balanceAfter(new BigDecimal("123456").subtract(request.getAmount()))
                .build();
    }
}

package com.woorifisan.platform.bank.service;

import com.woorifisan.platform.bank.dto.BalanceInquiryRequest;
import com.woorifisan.platform.bank.dto.BalanceInquiryResponse;
import com.woorifisan.platform.bank.dto.HistoryInquiryRequest;
import com.woorifisan.platform.bank.dto.HistoryInquiryResponse;
import com.woorifisan.platform.bank.dto.TransactionHistoryDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 계좌 조회 서비스 (더미 데이터 반환)
 */
@Slf4j
@Service
public class AccountInquiryService {

    /**
     * 잔액 조회 실행
     */
    public BalanceInquiryResponse getBalance(BalanceInquiryRequest request) {
        log.info("잔액 조회 요청 수신 - 계좌번호: {}", request.getAccountNo());

        // 명세서 기반 더미 데이터
        return BalanceInquiryResponse.builder()
                .balance(new java.math.BigDecimal("5420000"))
                .status("NORMAL")
                .build();
    }

    /**
     * 거래내역 조회 실행
     */
    public HistoryInquiryResponse getHistory(HistoryInquiryRequest request) {
        log.info("거래내역 조회 요청 수신 - 기간: {} ~ {}", request.getStartDate(), request.getEndDate());

        // 명세서 기반 더미 데이터
        List<TransactionHistoryDto> mockHistory = List.of(
                TransactionHistoryDto.builder()
                        .txId("TX20240519001")
                        .txDate("2024-05-18 14:20:05")
                        .txType("WITHDRAW")
                        .amount(new java.math.BigDecimal("50000"))
                        .balance(new java.math.BigDecimal("5420000"))
                        .counterpartName("김우리")
                        .description("ATM 출금")
                        .build(),
                TransactionHistoryDto.builder()
                        .txId("TX20240517002")
                        .txDate("2024-05-17 09:10:00")
                        .txType("DEPOSIT")
                        .amount(new java.math.BigDecimal("1200000"))
                        .balance(new java.math.BigDecimal("5470000"))
                        .counterpartName("(주)우리피앤에스")
                        .description("급여")
                        .build()
        );

        return HistoryInquiryResponse.builder()
                .totalCount(2)
                .totalPages(1)
                .currentPage(request.getPage())
                .size(request.getSize())
                .hasNext(false)
                .history(mockHistory)
                .build();
    }
}

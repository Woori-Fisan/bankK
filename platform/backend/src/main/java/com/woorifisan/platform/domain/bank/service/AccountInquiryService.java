package com.woorifisan.platform.domain.bank.service;

import com.woorifisan.platform.domain.bank.dto.request.BalanceInquiryRequest;
import com.woorifisan.platform.domain.bank.dto.request.HistoryInquiryRequest;
import com.woorifisan.platform.domain.bank.dto.response.BalanceInquiryResponse;
import com.woorifisan.platform.domain.bank.dto.response.HistoryInquiryResponse;
import com.woorifisan.platform.domain.bank.external.client.BankExternalClient;
import com.woorifisan.platform.domain.bank.external.dto.BankBalanceInquiryRequest;
import com.woorifisan.platform.domain.bank.external.dto.BankHistoryInquiryRequest;
import com.woorifisan.platform.global.exception.BusinessException;
import com.woorifisan.platform.global.response.ErrorCode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 계좌 조회 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AccountInquiryService {

    // 날짜 검증용 포맷
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final BankExternalClient bankExternalClient;

    /**
     * 잔액 조회 실행
     */
    public BalanceInquiryResponse getBalance(BalanceInquiryRequest request) {

        BankBalanceInquiryRequest bankRequest = BankBalanceInquiryRequest.of(
                request.getEncryptedKey(),
                request.getJwsSignature(),
                request.getAccountNo(),
                request.getCustomerRrnPrefix()
        );

        return bankExternalClient.fetchBalance(request.getBankCode(), bankRequest);
    }

    /**
     * 거래내역 조회 실행 (오케스트레이션)
     */
    public HistoryInquiryResponse getHistory(HistoryInquiryRequest request) {

        // 1. 날짜 검증
        validateInquiryPeriod(request.getStartDate(), request.getEndDate());

        // 2. 은행 서버로부터 데이터 취득 (통신 로직)
        BankHistoryInquiryRequest bankRequest = BankHistoryInquiryRequest.from(request);
        return bankExternalClient.fetchHistory(request.getBankCode(), bankRequest);
    }

    /**
     * 날짜 범위 검증
     */
    private void validateInquiryPeriod(String start, String end) {
        LocalDate startDate;
        LocalDate endDate;
        try {
            startDate = LocalDate.parse(start, DATE_FORMATTER);
            endDate = LocalDate.parse(end, DATE_FORMATTER);

        } catch (Exception e) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "날짜 형식이 올바르지 않습니다. (yyyy-MM-dd)");
        }

        // 시작날짜 마지막 날짜 순서 검증
        if (startDate.isAfter(endDate)) {
            throw new BusinessException(ErrorCode.INQUIRY_INVALID_DATE_RANGE);
        }
    }


}

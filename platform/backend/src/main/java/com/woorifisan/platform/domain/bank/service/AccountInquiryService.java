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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 계좌 조회 서비스 (E2EE Zero-Knowledge Pass-through)
 */
@Service
@RequiredArgsConstructor
public class AccountInquiryService {

    // 날짜 검증용 포맷
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final BankExternalClient bankExternalClient;

    /**
     * 잔액 조회 실행 (중계)
     */
    @Transactional(readOnly = true)
    public BalanceInquiryResponse getBalance(BalanceInquiryRequest request, String bankKeyId) {
        // 1. 은행 코어에 전달할 요청 DTO 생성 (Zero-Knowledge Pass-through)
        BankBalanceInquiryRequest bankRequest = BankBalanceInquiryRequest.of(
                request.getReqPayload(),
                bankKeyId
        );

        // 2. 외부 클라이언트를 통해 은행 코어 API 호출 및 결과 직접 반환
        return bankExternalClient.fetchBalance(request.getBankCode(), bankRequest);
    }

    /**
     * 거래내역 조회 실행 (중계)
     */
    @Transactional(readOnly = true)
    public HistoryInquiryResponse getHistory(HistoryInquiryRequest request, String bankKeyId) {
        // 1. 비민감 필드 검증 (날짜)
        validateInquiryPeriod(request.getStartDate(), request.getEndDate());

        // 2. 은행 서버로 전달할 요청 DTO 생성
        BankHistoryInquiryRequest bankRequest = BankHistoryInquiryRequest.of(request, bankKeyId);
        
        // 3. 외부 클라이언트 호출 및 결과 반환
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
package com.woorifisan.platform.domain.bank.service;

import com.woorifisan.platform.domain.bank.dto.request.BalanceInquiryRequest;
import com.woorifisan.platform.domain.bank.dto.response.BalanceInquiryResponse;
import com.woorifisan.platform.domain.bank.dto.request.HistoryInquiryRequest;
import com.woorifisan.platform.domain.bank.dto.response.HistoryInquiryResponse;
import com.woorifisan.platform.global.exception.BusinessException;
import com.woorifisan.platform.global.response.ApiResponse;
import com.woorifisan.platform.global.response.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.reactive.function.client.WebClient;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 계좌 조회 서비스
 */
@Slf4j
@Service
public class AccountInquiryService {

    // 날짜 포맷
    // 날짜 검증용 포맷
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private static final String BANK_SERVER_URL = "http://localhost:8081/api/v1/baas/account";
    
    private final WebClient bankWebClient;

    public AccountInquiryService(@Qualifier("bankWebClient") WebClient bankWebClient) {
        this.bankWebClient = bankWebClient;
    }

    /**
     * 잔액 조회 실행
     */
    public BalanceInquiryResponse getBalance(BalanceInquiryRequest request) {
        log.info("잔액 조회 요청 수신 - 계좌번호: {}", request.getAccountNo());
        // TODO: fetchBalanceFromBank(request) 호출로 전환 예정
        return BalanceInquiryResponse.builder()
                .balance(new java.math.BigDecimal("5420000"))
                .status("NORMAL")
                .build();
    }

    /**
     * 거래내역 조회 실행 (오케스트레이션)
     */
    @PostMapping(BANK_SERVER_URL+"/transactions")
    public HistoryInquiryResponse getHistory(HistoryInquiryRequest request) {
        log.info("거래내역 조회 요청 수신 - 계좌: {}, 기간: {} ~ {}", 
                 request.getAccountNo(), request.getStartDate(), request.getEndDate());

        // 1. 날짜 검증
        validateInquiryPeriod(request.getStartDate(), request.getEndDate());

        // 2. 은행 서버로부터 데이터 취득 (통신 로직)
        return fetchTransactionHistory(request);
    }

    /**
     * 날짜 범위 검증
     */
    private void validateInquiryPeriod(String start, String end) {
        LocalDate startDate = LocalDate.parse(start, DATE_FORMATTER);
        LocalDate endDate = LocalDate.parse(end, DATE_FORMATTER);
        
        if (startDate.isAfter(endDate)) {
            throw new BusinessException(ErrorCode.INQUIRY_INVALID_DATE_RANGE);
        }
    }

    /**
     * 은행 서버 API 호출 (실제 통신 담당)
     */
    private HistoryInquiryResponse fetchTransactionHistory(HistoryInquiryRequest request) {
        // [GEMINI.md 2.4] 블랙박스 로깅
        log.info("[TX_PAYLOAD_LOG] 은행 서버 요청 송신 - Account: {}, JWS: {}, EncryptedKey: {}", 
                 request.getAccountNo(), request.getJwsSignature(), request.getEncryptedKey());

        // Base 0 페이징 보장 (null이거나 음수일 경우 0으로 처리)
        int requestPage = (request.getPage() != null && request.getPage() >= 0) ? request.getPage() : 0;
        
        log.info("은행 서버 요청 파라미터 상세 - Page: {}, Size: {}, StartDate: {}, EndDate: {}",
                 requestPage, request.getSize(), request.getStartDate(), request.getEndDate());

        // Bank 서버 요청 진행
        ApiResponse<HistoryInquiryResponse> response;
        try {
            // [GEMINI.md 2.2] Zero-Knowledge (Pass-through)
            // 은행 서버의 응답 바디(ApiResponse)를 상태 코드와 관계없이 일단 수신
            response = bankWebClient.post()
                    .uri(BANK_SERVER_URL)
                    .bodyValue(request)
                    .exchangeToMono(clientResponse -> 
                        clientResponse.bodyToMono(new ParameterizedTypeReference<ApiResponse<HistoryInquiryResponse>>() {})
                    )
                    .block(java.time.Duration.ofSeconds(10));
        } catch (Exception e) {
            log.error("은행 서버 통신 중 예외 발생: {}", e.getMessage());
            throw new BusinessException(ErrorCode.BANK_API_ERROR);
        }

        // 1. 에러 체크 (은행 서버에서 success: false로 응답한 경우)
        if (response == null || !response.isSuccess()) {
            String errorMsg = (response != null && response.getError() != null)
                    ? response.getError().getMessage()
                    : "은행 서버로부터 응답을 받지 못했습니다.";

            log.warn("은행 비즈니스 로직 처리 실패 - 사유: {}", errorMsg);
            throw new BusinessException(ErrorCode.BANK_API_ERROR, errorMsg);
        }

        // 2. 정상 결과 처리
        HistoryInquiryResponse data = response.getData();
        log.info("은행 서버 응답 수신 성공 - TotalCount: {}, TotalPages: {}, CurrentPage: {}, HasNext: {}, HistorySize: {}",
                data.getTotalCount(), data.getTotalPages(), data.getCurrentPage(), data.getHasNext(),
                data != null && data.getHistory() != null ? data.getHistory().size() : 0);

        return data;
    }

}

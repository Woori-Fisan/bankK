package com.woorifisan.platform.domain.bank.service;

import com.woorifisan.platform.domain.bank.dto.request.BalanceInquiryRequest;
import com.woorifisan.platform.domain.bank.dto.request.HistoryInquiryRequest;
import com.woorifisan.platform.domain.bank.dto.response.BalanceInquiryResponse;
import com.woorifisan.platform.domain.bank.dto.response.HistoryInquiryResponse;
import com.woorifisan.platform.domain.bank.dto.response.TransactionHistoryDto;
import com.woorifisan.platform.domain.bank.external.client.BankExternalClient;
import com.woorifisan.platform.domain.bank.external.dto.BankBalanceInquiryRequest;
import com.woorifisan.platform.global.exception.BusinessException;
import com.woorifisan.platform.global.response.ErrorCode;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final BankExternalClient bankExternalClient;

    /**
     * 잔액 조회 실행
     */
    public BalanceInquiryResponse getBalance(BalanceInquiryRequest request) {
        log.info("잔액 조회 요청 수신 - 계좌번호: {}", request.getAccountNo());

        // Todo: 더미 로직 삭제
        if ("0000".equals(request.getAccountNo())) {
            throw new BusinessException(ErrorCode.TRANSFER_DEPOSIT_ACCOUNT_FAULT);
        }
        validateAccountAndBank(request.getAccountNo(), request.getBankCode());

        // 은행 시스템으로 보낼 요청 DTO 구성 (더미 보안 값 포함)
        BankBalanceInquiryRequest bankRequest = BankBalanceInquiryRequest.of(
                request.getEncryptedKey(),
                request.getJwsSignature(),
                request.getAccountNo(),
                request.getCustomerRrnPrefix()
        );

        return bankExternalClient.fetchBalance(request.getBankCode(), bankRequest);
    }

    /**
     * 거래내역 조회 실행 (더미 데이터 반환)
     */
    public HistoryInquiryResponse getHistory(HistoryInquiryRequest request) {
        log.info("거래내역 조회 요청 수신 - 기간: {} ~ {}, 페이지: {}, 사이즈: {}", 
                 request.getStartDate(), request.getEndDate(), request.getPage(), request.getSize());

        validateAccountAndBank(request.getAccountNo(), request.getBankCode());

        LocalDate startDate = LocalDate.parse(request.getStartDate(), DATE_FORMATTER);
        LocalDate endDate = LocalDate.parse(request.getEndDate(), DATE_FORMATTER);
        
        // 날짜 검증: 시작일이 종료일보다 뒤면 예외 발생
        if (startDate.isAfter(endDate)) {
            throw new BusinessException(ErrorCode.INQUIRY_INVALID_DATE_RANGE);
        }

        // Bank 서비스 구현 시 요청 값 저장으로 변경
        List<TransactionHistoryDto> allHistory = generateDummyDataBetween(startDate, endDate);
        
        // 최신 거래가 위로 오도록 정렬 (날짜 역순)
        Collections.reverse(allHistory);

        // 페이지네이션 처리
        int totalCount = allHistory.size();
        int size = request.getSize() != null && request.getSize() > 0 ? request.getSize() : 20;
        int page = request.getPage() != null && request.getPage() > 0 ? request.getPage() : 1;
        
        int totalPages = (int) Math.ceil((double) totalCount / size);
        
        int fromIndex = (page - 1) * size;
        int toIndex = Math.min(fromIndex + size, totalCount);

        List<TransactionHistoryDto> pagedHistory = new ArrayList<>();
        if (fromIndex < totalCount) {
            pagedHistory = allHistory.subList(fromIndex, toIndex);
        }

        boolean hasNext = page < totalPages;

        return HistoryInquiryResponse.builder()
                .totalCount(totalCount)
                .totalPages(totalPages)
                .currentPage(page)
                .size(size)
                .hasNext(hasNext)
                .history(pagedHistory)
                .build();
    }

    private HistoryInquiryResponse buildEmptyResponse(HistoryInquiryRequest request) {
        return HistoryInquiryResponse.builder()
            .totalCount(0)
            .totalPages(0)
            .currentPage(request.getPage())
            .size(request.getSize())
            .hasNext(false)
            .history(Collections.emptyList())
            .build();
    }

    /*
    * 더미 데이터 생성 코드
    */
    private List<TransactionHistoryDto> generateDummyDataBetween(LocalDate startDate, LocalDate endDate) {
        List<TransactionHistoryDto> dummyList = new ArrayList<>();
        BigDecimal currentBalance = new BigDecimal("10000000");
        
        long daysBetween = ChronoUnit.DAYS.between(startDate, endDate);
        
        int txCounter = 1;
        for (int i = 0; i <= daysBetween; i++) {
            LocalDate currentDate = startDate.plusDays(i);
            int txPerDay = (int) (Math.random() * 3) + 1;
            
            for (int j = 0; j < txPerDay; j++) {
                LocalDateTime txDateTime = currentDate.atTime(9 + j * 4, (int) (Math.random() * 59), 0);
                boolean isWithdraw = Math.random() < 0.7;
                String txType = isWithdraw ? "WITHDRAW" : "DEPOSIT";
                
                BigDecimal amount;
                String counterpart;
                String desc;
                
                if (isWithdraw) {
                    amount = new BigDecimal((int) (Math.random() * 5 + 1) * 1000);
                    currentBalance = currentBalance.subtract(amount);
                    counterpart = "우체국_체크카드";
                    desc = "물품구매";
                } else {
                    amount = new BigDecimal((int) (Math.random() * 15 + 5) * 100000);
                    currentBalance = currentBalance.add(amount);
                    counterpart = "(주)우리피앤에스";
                    desc = "급여/정산";
                }
                
                String txId = String.format("TX%s%04d", txDateTime.format(DateTimeFormatter.ofPattern("yyyyMMdd")), txCounter++);
                
                dummyList.add(TransactionHistoryDto.builder()
                    .txId(txId)
                    .txDate(txDateTime.format(DATETIME_FORMATTER))
                    .txType(txType)
                    .amount(amount)
                    .balance(currentBalance)
                    .counterpartName(counterpart)
                    .description(desc)
                    .build());
            }
        }
        
        return dummyList;
    }

    /**
     * 비즈니스 로직 검증 (더미 데이터 기준)
     */
    private void validateAccountAndBank(String accountNo, String bankCode) {
        if ("0000000000".equals(accountNo)) {
            throw new BusinessException(ErrorCode.INQUIRY_ACCOUNT_NOTFOUND);
        }
        if ("999".equals(bankCode)) {
            throw new BusinessException(ErrorCode.BANK_API_ERROR);
        }
    }
}

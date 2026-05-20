package com.woorifisan.platform.bank.service;

import com.woorifisan.platform.bank.dto.BalanceInquiryRequest;
import com.woorifisan.platform.bank.dto.BalanceInquiryResponse;
import com.woorifisan.platform.bank.dto.HistoryInquiryRequest;
import com.woorifisan.platform.bank.dto.HistoryInquiryResponse;
import com.woorifisan.platform.bank.dto.TransactionHistoryDto;
import com.woorifisan.platform.global.exception.BusinessException;
import com.woorifisan.platform.global.response.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 계좌 조회 서비스 (더미 데이터 반환)
 */
@Slf4j
@Service
public class AccountInquiryService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 잔액 조회 실행
     */
    public BalanceInquiryResponse getBalance(BalanceInquiryRequest request) {
        log.info("잔액 조회 요청 수신 - 계좌번호: {}", request.getAccountNo());

        validateAccountAndBank(request.getAccountNo(), request.getBankCode());

        // 명세서 기반 더미 데이터
        return BalanceInquiryResponse.builder()
                .balance(new BigDecimal("5420000"))
                .status("NORMAL")
                .build();
    }

    /**
     * 거래내역 조회 실행
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

    // 더미 데이터 생성
    private List<TransactionHistoryDto> generateDummyDataBetween(LocalDate startDate, LocalDate endDate) {
        List<TransactionHistoryDto> dummyList = new ArrayList<>();
        BigDecimal currentBalance = new BigDecimal("10000000"); // 초기 잔액 1000만 원 가정
        
        long daysBetween = ChronoUnit.DAYS.between(startDate, endDate);
        
        // 매일 1~3건의 더미 거래 생성
        int txCounter = 1;
        for (int i = 0; i <= daysBetween; i++) {
            LocalDate currentDate = startDate.plusDays(i);
            
            // 하루에 무작위로 1~3건 발생 (더미 로직)
            int txPerDay = (int) (Math.random() * 3) + 1;
            
            for (int j = 0; j < txPerDay; j++) {
                // 시간은 대충 낮 시간대로 분산
                LocalDateTime txDateTime = currentDate.atTime(9 + j * 4, (int) (Math.random() * 59), 0);
                
                // 입/출금 무작위 결정 (70% 확률로 출금)
                boolean isWithdraw = Math.random() < 0.7;
                String txType = isWithdraw ? "WITHDRAW" : "DEPOSIT";
                
                // 금액도 무작위 (출금은 1~5만 원, 입금은 50~200만 원)
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

    /**
     * 비즈니스 로직 검증 (더미 데이터 기준)
     */
    private void validateAccountAndBank(String accountNo, String bankCode) {
        // 더미 로직: 계좌번호가 "0000000000"인 경우 유효하지 않은 계좌 (INQUIRY_001)
        if ("0000000000".equals(accountNo)) {
            throw new BusinessException(ErrorCode.INQUIRY_ACCOUNT_NOTFOUND);
        }

        // 더미 로직: 은행 코드가 "999"인 경우 은행 API 에러 (BANK_002)
        if ("999".equals(bankCode)) {
            throw new BusinessException(ErrorCode.BANK_API_ERROR);
        }
    }
}

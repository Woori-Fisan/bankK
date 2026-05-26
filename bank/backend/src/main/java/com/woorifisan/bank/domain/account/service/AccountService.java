package com.woorifisan.bank.domain.account.service;

import com.woorifisan.bank.domain.account.dto.request.TransactionHistoryRequest;
import com.woorifisan.bank.domain.account.dto.response.TransactionHistoryDto;
import com.woorifisan.bank.domain.account.dto.response.TransactionHistoryResponse;
import com.woorifisan.bank.domain.account.dto.request.BalanceInquiryRequest;
import com.woorifisan.bank.domain.account.dto.response.BalanceInquiryResponse;
import com.woorifisan.bank.domain.account.mapper.AccountMapper;
import com.woorifisan.bank.domain.account.mapper.TransactionLedgerMapper;
import com.woorifisan.bank.domain.account.model.Account;
import com.woorifisan.bank.domain.account.model.TransactionLedger;
import com.woorifisan.bank.domain.customer.mapper.CustomerMapper;
import com.woorifisan.bank.domain.customer.model.Customer;
import com.woorifisan.bank.global.exception.BusinessException;
import com.woorifisan.bank.global.response.ErrorCode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 계좌 관련 비즈니스 로직을 처리하는 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AccountService {

    // 날짜 포맷
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final AccountMapper accountMapper;
    private final TransactionLedgerMapper transactionLedgerMapper;
    private final CustomerMapper customerMapper;

    /**
     * 거래 내역 조회
     */
    @Transactional(readOnly = true)
    public BalanceInquiryResponse getBalance(BalanceInquiryRequest request) {
        // 1. 계좌 및 고객 정보 검증 쿼리 호출
        Account account = accountMapper.findByAccountNoAndRrnPrefix(
                request.getAccountNo(),
                request.getCustomerRrnPrefix()
        ).orElseThrow(() -> new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND));

        // 2. 응답 반환
        return BalanceInquiryResponse.builder()
                .balance(account.getBalance())
                .status(account.getStatus())
                .build();
    }

    /**
     * 거래 내역 조회
     */
    @Transactional(readOnly = true)
    public TransactionHistoryResponse getTransactionHistoryList(TransactionHistoryRequest request) {
        log.info("은행 서버 거래내역 조회 요청 수신 - 계좌해시: {}, 기간: {} ~ {}, 페이지: {}, 사이즈: {}",
                request.getAccountNo(), request.getStartDate(), request.getEndDate(), request.getPage(), request.getSize());

        // 1. 날짜 검증
        validateInquiryPeriod(request.getStartDate(), request.getEndDate());

        // 2. 계좌 조회 (Blind Index 활용)
        // TODO: 실제 환경에서는 request.getAccountNo()를 복호화한 후 SHA-256 해싱하여 검색해야 함
        // 현재는 간단한 세팅을 위해 입력받은 값을 그대로 해시로 가정하거나 임시 로직으로 처리
        String accountNoHash = request.getAccountNo();

        Account account = accountMapper.findByAccountNo(accountNoHash)
                .orElseThrow(() -> new BusinessException(ErrorCode.INQUIRY_ACCOUNT_NOTFOUND));

        // 3. 계좌 소유주 일치 확인
        Customer customer = customerMapper.findById(account.getCustomerId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (!customer.getRrnPrefix().equals(request.getCustomerRrnPrefix())) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        // 4. 전체 건수 조회
        int totalCount = transactionLedgerMapper.countHistory(
                account.getId(),
                request.getStartDate(),
                request.getEndDate()
        );

        // 5. 페이징된 목록 조회
        int offset = request.getPage() * request.getSize();
        List<TransactionLedger> ledgerList = transactionLedgerMapper.findHistoryList(
                account.getId(),
                request.getStartDate(),
                request.getEndDate(),
                offset,
                request.getSize()
        );

        // 6. DTO 변환 및 반환
        return convertToResponse(request, totalCount, ledgerList);
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

    private TransactionHistoryResponse convertToResponse(TransactionHistoryRequest request, int totalCount, List<TransactionLedger> ledgerList) {
        int totalPages = (int) Math.ceil((double) totalCount / request.getSize());
        boolean hasNext = request.getPage() < totalPages - 1;

        List<TransactionHistoryDto> historyItems = ledgerList.stream()
                .map(ledger -> TransactionHistoryDto.builder()
                        .txId(ledger.getTxId())
                        .txDate(ledger.getTransactedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                        .txType(ledger.getTxType())
                        .amount(ledger.getAmount())
                        .balance(ledger.getBalanceAfter())
                        .counterpartName(ledger.getTargetAccount() != null ? ledger.getTargetAccount() : "")
                        .description(ledger.getDescription())
                        .build())
                .collect(Collectors.toList());

        return TransactionHistoryResponse.builder()
                .totalCount(totalCount)
                .totalPages(totalPages)
                .currentPage(request.getPage())
                .size(request.getSize())
                .hasNext(hasNext)
                .history(historyItems)
                .build();
    }
}

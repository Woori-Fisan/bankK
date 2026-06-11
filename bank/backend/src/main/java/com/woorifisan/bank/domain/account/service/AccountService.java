package com.woorifisan.bank.domain.account.service;

import com.woorifisan.bank.domain.account.dto.decrypted.DecryptedInquiryData;
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
import com.woorifisan.bank.domain.customer.service.CustomerService;
import com.woorifisan.bank.global.exception.BusinessException;
import com.woorifisan.bank.global.response.ErrorCode;
import com.woorifisan.bank.global.security.service.SecurityService;
import com.woorifisan.bank.global.util.CryptoUtil;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 계좌 관련 비즈니스 로직을 처리하는 서비스 (E2EE 암복호화 적용)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AccountService {

    // 날짜 비교용 포맷
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    // 출력용 포맷
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final AccountMapper accountMapper;
    private final TransactionLedgerMapper transactionLedgerMapper;
    private final CustomerService customerService;
    private final SecurityService securityService;
    private final CryptoUtil cryptoUtil;

    /**
     * 잔액 조회 (E2EE 적용)
     */
    @Transactional(readOnly = true)
    public BalanceInquiryResponse getBalance(BalanceInquiryRequest request) {
        log.info("은행 서버 잔액 조회 요청 수신 - 키ID: {}", request.getBankKeyId());

        // 1. 복호화 및 CEK(Content Encryption Key) 추출
        // RSA 개인키로 세션키를 복호화하고, 세션키로 페이로드를 복호화합니다.
        SecurityService.DecryptionResult<DecryptedInquiryData> decryptionResult = 
                securityService.decryptWithKey(request, DecryptedInquiryData.class);
        
        DecryptedInquiryData decryptedData = decryptionResult.getData();

        // 2. 계좌 조회 (Blind Index 활용)
        // 평문 계좌번호를 해싱하여 DB의 account_no_hash 컬럼과 매칭합니다.
        Account account = accountMapper.findByAccountNoHash(cryptoUtil.hash(decryptedData.getAccountNo()))
                .orElseThrow(() -> new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND));

        // 3. 소유주 확인
        // 계좌 소유자의 고객 정보와 요청 데이터(이름, 주민번호 앞자리)가 일치하는지 검증합니다.
        customerService.verifyCustomerIdentification(account.getCustomerId(), decryptedData.getCustomerRrnPrefix(), decryptedData.getCustomerName());

        // 4. 응답 데이터 암호화 (추출된 CEK 사용)
        // 민감 정보(잔액)를 세션키로 암호화하여 응답 페이로드를 생성합니다.
        BalanceInquiryResponse.SensitiveData sensitiveData = BalanceInquiryResponse.SensitiveData.builder()
                .balance(account.getBalance())
                .build();
        
        String resPayload = securityService.encryptResponse(sensitiveData, decryptionResult.getCek());

        // 5. 응답 반환 (민감 정보는 resPayload에, 나머지는 평문)
        return BalanceInquiryResponse.of(resPayload, account.getStatus());
    }

    /**
     * 거래 내역 조회 (E2EE 적용)
     */
    @Transactional(readOnly = true)
    public TransactionHistoryResponse getTransactionHistoryList(TransactionHistoryRequest request) {
        log.info("은행 서버 거래내역 조회 요청 수신 - 키ID: {}, 기간: {} ~ {}",
                request.getBankKeyId(), request.getStartDate(), request.getEndDate());

        // 1. 조회 기간 유효성 검증
        validateInquiryPeriod(request.getStartDate(), request.getEndDate());

        // 2. E2EE 복호화 및 세션키 추출
        SecurityService.DecryptionResult<DecryptedInquiryData> decryptionResult = 
                securityService.decryptWithKey(request, DecryptedInquiryData.class);
        
        DecryptedInquiryData decryptedData = decryptionResult.getData();

        // 3. 계좌 조회 (Blind Index 활용)
        Account account = accountMapper.findByAccountNoHash(cryptoUtil.hash(decryptedData.getAccountNo()))
                .orElseThrow(() -> new BusinessException(ErrorCode.INQUIRY_ACCOUNT_NOTFOUND));

        // 4. 계좌 소유주 일치 확인
        customerService.verifyCustomerIdentification(account.getCustomerId(), decryptedData.getCustomerRrnPrefix(), decryptedData.getCustomerName());

        // 5. 전체 거래 건수 조회
        int totalCount = transactionLedgerMapper.countHistory(
                account.getId(),
                request.getStartDate(),
                request.getEndDate()
        );

        // 6. 페이징 처리된 거래 목록 조회
        int offset = request.getPage() * request.getSize();
        List<TransactionLedger> ledgerList = transactionLedgerMapper.findHistoryList(
                account.getId(),
                request.getStartDate(),
                request.getEndDate(),
                offset,
                request.getSize()
        );

        // 7. 거래 내역 리스트 생성 및 암호화
        // 거래 내역 전체를 응답 페이로드로 구성하여 세션키로 암호화합니다.
        List<TransactionHistoryDto> historyItems = ledgerList.stream()
                .map(ledger -> TransactionHistoryDto.builder()
                        .txId(ledger.getTxId())
                        .txDate(ledger.getTransactedAt().format(DATE_TIME_FORMATTER))
                        .txType(ledger.getTxType())
                        .amount(ledger.getAmount())
                        .balance(ledger.getBalanceAfter())
                        .counterpartName(ledger.getTargetAccount() != null ? ledger.getTargetAccount() : "")
                        .description(ledger.getDescription())
                        .build())
                .collect(Collectors.toList());

        TransactionHistoryResponse.SensitiveData sensitiveData = TransactionHistoryResponse.SensitiveData.builder()
                .history(historyItems)
                .build();
        
        String resPayload = securityService.encryptResponse(sensitiveData, decryptionResult.getCek());

        // 8. DTO 변환 및 최종 응답 반환
        int totalPages = (int) Math.ceil((double) totalCount / request.getSize());
        boolean hasNext = request.getPage() < totalPages - 1;

        return TransactionHistoryResponse.of(
                resPayload, totalCount, totalPages, request.getPage(), request.getSize(), hasNext
        );
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
}

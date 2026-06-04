package com.woorifisan.bank.domain.loan.service;

import com.woorifisan.bank.domain.account.mapper.AccountMapper;
import com.woorifisan.bank.domain.account.mapper.TransactionLedgerMapper;
import com.woorifisan.bank.domain.account.model.Account;
import com.woorifisan.bank.domain.account.model.TransactionLedger;
import com.woorifisan.bank.domain.customer.mapper.CustomerMapper;
import com.woorifisan.bank.domain.customer.model.Customer;
import com.woorifisan.bank.domain.document.mapper.CommonDocumentMapper;
import com.woorifisan.bank.domain.document.model.CommonDocument;
import com.woorifisan.bank.domain.loan.dto.decrypted.DecryptedLoanEvaluateRequest;
import com.woorifisan.bank.domain.loan.dto.decrypted.DecryptedLoanExecuteRequest;
import com.woorifisan.bank.domain.loan.dto.request.LoanEvaluateRequest;
import com.woorifisan.bank.domain.loan.dto.request.LoanExecuteRequest;
import com.woorifisan.bank.domain.loan.dto.response.LoanAcceptResponse;
import com.woorifisan.bank.domain.loan.dto.response.LoanEvaluationStatusResponse;
import com.woorifisan.bank.domain.loan.dto.response.LoanExecuteResponse;
import com.woorifisan.bank.domain.loan.dto.response.LoanProductResponse;
import com.woorifisan.bank.domain.loan.dto.response.SensitiveLoanExecuteResponse;
import com.woorifisan.bank.domain.loan.dto.response.TermsResponse;
import com.woorifisan.bank.domain.loan.mapper.LoanLedgerMapper;
import com.woorifisan.bank.domain.loan.mapper.LoanProductMapper;
import com.woorifisan.bank.domain.loan.model.LoanLedger;
import com.woorifisan.bank.domain.loan.model.LoanProduct;
import com.woorifisan.bank.domain.terms.mapper.BankTermsMapper;
import com.woorifisan.bank.global.exception.BusinessException;
import com.woorifisan.bank.global.response.ErrorCode;
import com.woorifisan.bank.global.security.service.SecurityService;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import javax.crypto.SecretKey;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoanService {

    @Value("${bank.code}")
    private String bankCode;

    @Value("${bank.document.storage.path}")
    private String documentStoragePath;

    private static final BigDecimal MAX_LOAN_AMOUNT = new BigDecimal("100000000");

    private final CustomerMapper customerMapper;
    private final AccountMapper accountMapper;
    private final TransactionLedgerMapper transactionLedgerMapper;
    private final LoanLedgerMapper loanLedgerMapper;
    private final LoanProductMapper loanProductMapper;
    private final BankTermsMapper bankTermsMapper;
    private final CommonDocumentMapper commonDocumentMapper;
    private final BCryptPasswordEncoder passwordEncoder;
    private final LoanReviewAsyncService loanReviewAsyncService;
    private final SecurityService securityService;

    // 심사 약관 조회
    @Transactional(readOnly = true)
    public List<TermsResponse> getEvaluationTerms() {
        return bankTermsMapper.findActiveByTermsType("EVALUATION").stream()
                .map(TermsResponse::from)
                .toList();
    }

    // 계약 약관 조회
    @Transactional(readOnly = true)
    public List<TermsResponse> getContractTerms(Long productId, String loanNo) {
        // APPROVED 건에만 계약 약관을 내려줌
        LoanLedger ledger = loanLedgerMapper.findByLoanNo(loanNo)
                .orElseThrow(() -> new BusinessException(ErrorCode.LOAN_NOT_FOUND));
        if (!"APPROVED".equals(ledger.getStatus())) {
            throw new BusinessException(ErrorCode.LOAN_INVALID_STATUS);
        }
        loanProductMapper.findByProductId(productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.LOAN_PRODUCT_NOT_FOUND));
        return bankTermsMapper.findActiveByTermsType("CONTRACT").stream()
                .map(TermsResponse::from)
                .toList();
    }

    // 대출 접수 (동기) + 비동기 심사 시작
    // 이 메서드는 빠르게 반환하고 실제 심사는 processReview() 가 @Async 로 처리하기!
    @Transactional
    public LoanAcceptResponse acceptLoan(LoanEvaluateRequest request, List<MultipartFile> files) {

        // 0. 복호화 및 민감 정보 획득 (CEK 추출 포함)
        SecurityService.DecryptionResult<DecryptedLoanEvaluateRequest> result =
                securityService.decryptWithKey(request, DecryptedLoanEvaluateRequest.class);
        DecryptedLoanEvaluateRequest decrypted = result.getData();
        final SecretKey cek = result.getCek();

        // 1. 약관 동의 플래그 검증 — 세 가지 모두 true 여야 접수 가능
        if (!Boolean.TRUE.equals(request.getIsCreditInfoAgreed())
                || !Boolean.TRUE.equals(request.getIsProductTermsAgreed())
                || !Boolean.TRUE.equals(request.getIsDocumentCollected())) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }

        // 대출 기간 0 이하면 금융 계산(DSR, 월납입금)이 0으로 흘러 잘못된 승인이 날 수 있음
        if (request.getRequestedPeriod() == null || request.getRequestedPeriod() <= 0) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }

        // 2. 파일명 기반 서류 종류 검증 (신분증, 재직증명서, 원천징수, 건강보험)
        validateFileNames(files);

        // 3. 입금 계좌 은행이 이 Bank 인지 확인. 타행 계좌로는 입금 불가
        if (!Objects.equals(bankCode, request.getDepositBankCode())) {
            throw new BusinessException(ErrorCode.LOAN_DEPOSIT_BANK_MISMATCH);
        }

        // 4. 입금 계좌 조회 및 상태 확인
        Account account = accountMapper.findByAccountNoPlain(decrypted.getDepositAccountNo())
                .orElseThrow(() -> new BusinessException(ErrorCode.LOAN_ACCOUNT_NOT_FOUND));
        if ("LOCKED".equals(account.getStatus())) {
            throw new BusinessException(ErrorCode.LOAN_ACCOUNT_LOCKED);
        } else if (!"NORMAL".equals(account.getStatus())) {
            throw new BusinessException(ErrorCode.LOAN_ACCOUNT_ABNORMAL);
        }

        // 5. 계좌 소유자와 요청 고객 정보 일치 여부 확인 (본인 확인)
        Customer customer = customerMapper.findById(account.getCustomerId())
                .orElseThrow(() -> new BusinessException(ErrorCode.LOAN_CUSTOMER_NOT_FOUND));
        if (!customer.getRrnPrefix().equals(decrypted.getCustomerRrnPrefix())
                || !customer.getCustomerName().equals(decrypted.getCustomerName())) {
            throw new BusinessException(ErrorCode.LOAN_CUSTOMER_IDENTITY_MISMATCH);
        }

        // 6. 동일 고객의 SUBMITTED 건 존재 시 중복 신청 차단
        if (loanLedgerMapper.existsPendingByCustomerId(customer.getId())) {
            throw new BusinessException(ErrorCode.LOAN_DUPLICATE_PENDING);
        }

        String loanNo = generateLoanNo();

        // 7. 파일을 on-premises 스토리지에 저장 (E2EE 복호화 포함). 실패 시 저장된 파일 cleanup 후 예외
        List<Path> savedPaths = saveFiles(loanNo, files, cek);

        // 8. loan_ledger SUBMITTED 상태로 생성
        LoanLedger ledger = LoanLedger.builder()
                .loanNo(loanNo)
                .customerId(customer.getId())
                .linkedAccountId(account.getId())
                .requestedAmount(request.getRequestedAmount())
                .requestedPeriod(request.getRequestedPeriod())
                .isCreditInfoAgreed(request.getIsCreditInfoAgreed())
                .isProductTermsAgreed(request.getIsProductTermsAgreed())
                .isDocumentCollected(request.getIsDocumentCollected())
                .status("SUBMITTED")
                .build();
        loanLedgerMapper.insert(ledger);

        // 9. common_document 에 저장된 파일 경로 기록
        for (int i = 0; i < files.size(); i++) {
            commonDocumentMapper.insertDocument(CommonDocument.ofLoan(
                    ledger.getId(),
                    "LOAN_DOCUMENT",
                    files.get(i).getOriginalFilename(),
                    savedPaths.get(i).toString()));
        }

        log.info("[접수] 대출 접수 완료 - loanNo: {}, requestKey: {}", loanNo, request.getRequestKey());

        // 10. 트랜잭션 커밋 후 비동기 심사 시작.
        // @Transactional 메서드 안에서 @Async를 바로 호출하면 커밋 전에 다른 스레드가 실행되어
        // findByLoanNo 조회 시 아직 INSERT가 반영되지 않은 상태일 수 있음 (race condition).
        // afterCommit()으로 DB에 loan_ledger가 확정된 뒤에 심사 스레드를 시작한다.
        final String finalLoanNo = loanNo;
        final String finalRequestKey = request.getRequestKey();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                loanReviewAsyncService.processReview(finalLoanNo, finalRequestKey, cek);
            }
        });

        return LoanAcceptResponse.builder()
                .loanNo(loanNo)
                .status("SUBMITTED")
                .build();
    }

    // 파일을 {documentStoragePath}/{loanNo}/ 경로에 저장
    // 중간에 실패하면 이미 저장된 파일을 모두 삭제하고 예외 던지기
    private List<Path> saveFiles(String loanNo, List<MultipartFile> files, SecretKey cek) {
        List<Path> savedPaths = new ArrayList<>();
        Path loanDir = Paths.get(documentStoragePath, loanNo);
        try {
            Files.createDirectories(loanDir);
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.LOAN_STORAGE_ERROR);
        }

        for (MultipartFile file : files) {
            // Paths.get().getFileName()으로 경로 컴포넌트 제거 — Path Traversal(CWE-22) 방지
            String originalFilename = file.getOriginalFilename();
            String cleanFileName = (originalFilename != null)
                    ? Paths.get(originalFilename).getFileName().toString()
                    : "unnamed";
            String fileName = UUID.randomUUID() + "_" + cleanFileName;
            Path target = loanDir.resolve(fileName);
            
            try {
                // E2EE 복호화: MultipartFile에서 바이트 배열을 읽어 복호화 수행
                byte[] encryptedBytes = file.getBytes();
                byte[] decryptedBytes = securityService.decryptFile(encryptedBytes, cek);
                
                // 복호화된 원본 데이터를 파일로 저장
                Files.write(target, decryptedBytes);
                savedPaths.add(target);
            } catch (IOException | RuntimeException e) {
                // 저장 성공한 파일들 전부 삭제 후 예외
                savedPaths.forEach(p -> {
                    try { Files.deleteIfExists(p); } catch (IOException ignored) {}
                });
                try { Files.deleteIfExists(loanDir); } catch (IOException ignored) {}
                log.error("[접수] 파일 복호화 및 저장 실패 - loanNo: {}, file: {}", loanNo, file.getOriginalFilename(), e);
                throw new BusinessException(ErrorCode.LOAN_STORAGE_ERROR);
            }
        }
        return savedPaths;
    }

    // 파일명에 필수 서류 키워드가 포함되어 있는지 검증
    private static final java.util.Map<String, List<String>> REQUIRED_DOCUMENT_KEYWORDS =
            java.util.Map.of(
                    "신분증 사본",           List.of("신분증", "면허증"),
                    "재직증명서",            List.of("재직증명서"),
                    "근로소득 원천징수영수증", List.of("원천징수"),
                    "건강보험료 납부확인서",   List.of("건강보험")
            );

    private void validateFileNames(List<MultipartFile> files) {
        List<String> fileNames = (files == null) ? List.of() :
                files.stream()
                        .filter(Objects::nonNull)
                        .map(MultipartFile::getOriginalFilename)
                        .filter(Objects::nonNull)
                        .map(String::toLowerCase)
                        .toList();

        List<String> missing = new ArrayList<>();
        for (java.util.Map.Entry<String, List<String>> entry : REQUIRED_DOCUMENT_KEYWORDS.entrySet()) {
            boolean covered = fileNames.stream()
                    .anyMatch(name -> entry.getValue().stream().anyMatch(name::contains));
            if (!covered) missing.add(entry.getKey());
        }
        if (!missing.isEmpty()) {
            throw new BusinessException(ErrorCode.LOAN_MISSING_REQUIRED_DOCUMENTS,
                    "필수 서류가 누락되었습니다: " + String.join(", ", missing));
        }
    }

    // 심사 상태 Polling
    // SSE 기반 아키텍처에서는 사용되지 않지만, 클라이언트가 직접 상태를 확인하고 싶을 때를 위해 유지
    @Transactional(readOnly = true)
    public LoanEvaluationStatusResponse getEvaluationStatus(Long evaluationId) {
        LoanLedger ledger = loanLedgerMapper.findById(evaluationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.LOAN_NOT_FOUND));
        return LoanEvaluationStatusResponse.from(ledger, ledger.getApprovedLimit());
    }

    // 대출 실행
    @Transactional
    public LoanExecuteResponse executeLoan(LoanExecuteRequest request) {

        // 0. 복호화 및 민감 정보 획득 (CEK 추출 포함)
        SecurityService.DecryptionResult<DecryptedLoanExecuteRequest> result =
                securityService.decryptWithKey(request, DecryptedLoanExecuteRequest.class);
        DecryptedLoanExecuteRequest decrypted = result.getData();

        // 1. 상태 검증: APPROVED 건만 실행 가능
        LoanLedger loanLedger = loanLedgerMapper.findByLoanNo(request.getLoanNo())
                .orElseThrow(() -> new BusinessException(ErrorCode.LOAN_NOT_FOUND));
        if ("ACTIVE".equals(loanLedger.getStatus())) {
            throw new BusinessException(ErrorCode.LOAN_ALREADY_EXECUTED);   // 중복 실행 방지
        }
        if (!"APPROVED".equals(loanLedger.getStatus())) {
            throw new BusinessException(ErrorCode.LOAN_INVALID_STATUS);
        }

        // 대출 기간 0 이하면 월납입금 계산 및 만기일 산정이 잘못됨
        if (request.getRepaymentPeriod() == null || request.getRepaymentPeriod() <= 0) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }

        // 2. 실행 금액이 승인 한도 초과 여부 확인
        LoanProduct product = loanProductMapper.findByProductId(request.getProductId())
                .orElseThrow(() -> new BusinessException(ErrorCode.LOAN_PRODUCT_NOT_FOUND));
        if (request.getLoanAmount().compareTo(loanLedger.getApprovedLimit()) > 0) {
            throw new BusinessException(ErrorCode.LOAN_EXCEED_APPROVED_LIMIT);
        }

        // 3. 불완전판매 방지: 동일 고객·상품 60일 내 ACTIVE 대출이 있으면 차단
        if (loanLedgerMapper.existsRecentActiveByCustomerAndProduct(
                loanLedger.getCustomerId(), product.getProductId())) {
            throw new BusinessException(ErrorCode.LOAN_INCOMPLETE_SALE_PREVENTION);
        }

        // 4. 입금 계좌 상태 및 비밀번호 검증
        // 잔액 업데이트 전 다른 트랜잭션의 동시 접근 차단
        Account account = accountMapper.findByIdForUpdate(loanLedger.getLinkedAccountId())
                .orElseThrow(() -> new BusinessException(ErrorCode.LOAN_ACCOUNT_NOT_FOUND));

        // 복호화된 계좌번호와 원장의 연결 계좌번호 일치 확인
        if (!account.getAccountNo().equals(decrypted.getDepositAccountNo())) {
            throw new BusinessException(ErrorCode.LOAN_ACCOUNT_NOT_FOUND);
        }

        if ("LOCKED".equals(account.getStatus())) {
            throw new BusinessException(ErrorCode.LOAN_ACCOUNT_LOCKED);
        } else if (!"NORMAL".equals(account.getStatus())) {
            throw new BusinessException(ErrorCode.LOAN_ACCOUNT_ABNORMAL);
        }
        // bcrypt 해시와 비교
        if (!passwordEncoder.matches(decrypted.getAccountPassword(), account.getPassword())) {
            throw new BusinessException(ErrorCode.LOAN_ACCOUNT_PASSWORD_MISMATCH);
        }

        Customer customer = customerMapper.findById(account.getCustomerId())
                .orElseThrow(() -> new BusinessException(ErrorCode.LOAN_CUSTOMER_NOT_FOUND));

        // 5. 월 납입금 계산 (원리금균등상환 공식)
        BigDecimal interestRate = loanLedger.getInterestRate();
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = startDate.plusMonths(request.getRepaymentPeriod());
        BigDecimal monthlyPayment = loanReviewAsyncService.calculateMonthlyPayment(
                request.getLoanAmount(), interestRate, request.getRepaymentPeriod());

        // 6. loan_ledger 실행 정보 확정 및 입금 계좌 잔액 증가
        LoanLedger forUpdate = LoanLedger.builder()
                .loanNo(request.getLoanNo())
                .productId(product.getProductId())
                .loanAmount(request.getLoanAmount())
                .interestRate(interestRate)
                .repaymentType(request.getRepaymentType())
                .repaymentPeriod(request.getRepaymentPeriod())
                .startDate(startDate)
                .endDate(endDate)
                .build();
        loanLedgerMapper.updateExecution(forUpdate);

        int updatedCount = accountMapper.updateBalance(account.getId(), request.getLoanAmount(), account.getVersion());
        if (updatedCount == 0) {
            throw new BusinessException(ErrorCode.CONCURRENT_MODIFICATION);
        }

        // 7. 거래 원장 기록 (대출 실행 내역)
        BigDecimal balanceAfter = account.getBalance().add(request.getLoanAmount());
        String txId = "LOAN-" + UUID.randomUUID().toString().replace("-", "").toUpperCase();
        transactionLedgerMapper.insert(TransactionLedger.of(
                txId, account.getId(), "LOAN", request.getLoanAmount(), balanceAfter,
                null, null, product.getProductName() + " 대출 실행", "SUCCESS"));

        // 8. 민감 정보 암호화 (추출된 CEK 사용)
        SensitiveLoanExecuteResponse sensitive = new SensitiveLoanExecuteResponse(customer.getCustomerName());
        String resPayload = securityService.encryptResponse(sensitive, result.getCek());

        return LoanExecuteResponse.builder()
                .resPayload(resPayload)
                .loanNo(request.getLoanNo())
                .loanAmount(request.getLoanAmount())
                .interestRate(interestRate)
                .repaymentType(request.getRepaymentType())
                .repaymentPeriod(request.getRepaymentPeriod())
                .monthlyPayment(monthlyPayment)
                .startDate(startDate)
                .endDate(endDate)
                .linkedAccountId(account.getId())
                .build();
    }

    // 상품 목록
    @Transactional(readOnly = true)
    public List<LoanProductResponse> getActiveProducts() {
        return loanProductMapper.findAllActive().stream()
                .map(LoanProductResponse::from)
                .toList();
    }

    // "LN-" + UUID 앞 16자리 대문자
    private String generateLoanNo() {
        return "LN-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
    }
}

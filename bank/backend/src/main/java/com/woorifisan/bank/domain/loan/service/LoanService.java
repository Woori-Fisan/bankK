package com.woorifisan.bank.domain.loan.service;

import com.woorifisan.bank.domain.account.mapper.AccountMapper;
import com.woorifisan.bank.domain.account.mapper.TransactionLedgerMapper;
import com.woorifisan.bank.domain.account.model.Account;
import com.woorifisan.bank.domain.account.model.TransactionLedger;
import com.woorifisan.bank.domain.customer.mapper.CustomerMapper;
import com.woorifisan.bank.domain.customer.model.Customer;
import com.woorifisan.bank.domain.loan.dto.request.LoanEvaluateRequest;
import com.woorifisan.bank.domain.loan.dto.request.LoanExecuteRequest;
import com.woorifisan.bank.domain.loan.dto.response.AvailableProductDto;
import com.woorifisan.bank.domain.loan.dto.response.LoanEvaluateResponse;
import com.woorifisan.bank.domain.loan.dto.response.LoanEvaluationStatusResponse;
import com.woorifisan.bank.domain.loan.dto.response.LoanExecuteResponse;
import com.woorifisan.bank.domain.loan.dto.response.LoanProductResponse;
import com.woorifisan.bank.domain.loan.dto.response.TermsResponse;
import com.woorifisan.bank.domain.loan.mapper.LoanLedgerMapper;
import com.woorifisan.bank.domain.loan.mapper.LoanProductMapper;
import com.woorifisan.bank.domain.loan.model.LoanLedger;
import com.woorifisan.bank.domain.loan.model.LoanProduct;
import com.woorifisan.bank.domain.terms.mapper.BankTermsMapper;
import com.woorifisan.bank.global.exception.BusinessException;
import com.woorifisan.bank.global.response.ErrorCode;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LoanService {

    private static final BigDecimal MOCK_ANNUAL_INCOME = new BigDecimal("36000000");
    private static final BigDecimal DSR_LIMIT = new BigDecimal("40");
    private static final BigDecimal BASE_RATE = new BigDecimal("3.50");
    private static final int CREDIT_SCORE_MIN = 600;
    private static final BigDecimal MAX_LOAN_AMOUNT = new BigDecimal("100000000");

    private final CustomerMapper customerMapper;
    private final AccountMapper accountMapper;
    private final TransactionLedgerMapper transactionLedgerMapper;
    private final LoanLedgerMapper loanLedgerMapper;
    private final LoanProductMapper loanProductMapper;
    private final BankTermsMapper bankTermsMapper;
    private final BCryptPasswordEncoder passwordEncoder;

    // ── BK-B11: 심사 약관 조회 ────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<TermsResponse> getEvaluationTerms() {
        return bankTermsMapper.findActiveByTermsType("EVALUATION").stream()
                .map(TermsResponse::from)
                .toList();
    }

    // ── BK-B20: 계약 약관 조회 ────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<TermsResponse> getContractTerms(Long productId, Long evaluationId) {
        LoanLedger ledger = loanLedgerMapper.findById(evaluationId)
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

    // ── BK-B13 ~ B19: 대출 심사 ───────────────────────────────────────────────

    @Transactional
    public LoanEvaluateResponse evaluateLoan(LoanEvaluateRequest request) {
        if (!Boolean.TRUE.equals(request.getIsCreditInfoAgreed())
                || !Boolean.TRUE.equals(request.getIsProductTermsAgreed())
                || !Boolean.TRUE.equals(request.getIsDocumentCollected())) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }

        // BK-B21: 계좌 유효성 검증 (심사 단계에서 입금 계좌 사전 확인)
        Account account = accountMapper.findByAccountNoPlain(request.getDepositAccountNo())
                .orElseThrow(() -> new BusinessException(ErrorCode.LOAN_ACCOUNT_NOT_FOUND));

        if (!"NORMAL".equals(account.getStatus())) {
            throw new BusinessException(ErrorCode.LOAN_ACCOUNT_ABNORMAL);
        }

        Customer customer = customerMapper.findById(account.getCustomerId())
                .orElseThrow(() -> new BusinessException(ErrorCode.LOAN_CUSTOMER_NOT_FOUND));

        // BKC04: 주민번호 앞 7자리 본인 확인
        if (!request.getCustomerRrnPrefix().equals(customer.getRrnPrefix())) {
            throw new BusinessException(ErrorCode.LOAN_CUSTOMER_IDENTITY_MISMATCH);
        }

        String loanNo = generateLoanNo();

        // BK-B13: NICE Mock 신용점수 조회
        int creditScore = getMockCreditScore(customer.getId());

        // BK-B14: 신용점수 컷 600점 미만 REJECTED
        if (creditScore < CREDIT_SCORE_MIN) {
            LoanLedger saved = saveLoanLedger(loanNo, customer.getId(), account.getId(), request,
                    creditScore, BigDecimal.ZERO, BigDecimal.ZERO, null, "REJECTED",
                    "신용점수 미달 (" + creditScore + "점)");
            return LoanEvaluateResponse.rejected(saved.getId(), loanNo,
                    creditScore, BigDecimal.ZERO, "신용점수 미달 (" + creditScore + "점)");
        }

        // BK-B17: 금리 산출 + 법정최고금리 20% 초과 시 REJECTED
        BigDecimal appliedRate = calculateAppliedRate(creditScore);

        if (appliedRate.compareTo(new BigDecimal("20")) > 0) {
            LoanLedger saved = saveLoanLedger(loanNo, customer.getId(), account.getId(), request,
                    creditScore, BigDecimal.ZERO, BigDecimal.ZERO, appliedRate, "REJECTED",
                    "법정최고금리 초과 (" + appliedRate + "%)");
            return LoanEvaluateResponse.rejected(saved.getId(), loanNo,
                    creditScore, BigDecimal.ZERO, "법정최고금리 초과 (" + appliedRate + "%)");
        }

        List<LoanLedger> activeLoans = loanLedgerMapper.findActiveByCustomerId(customer.getId());

        // BK-B15: DSR 계산, 40% 초과 REJECTED
        BigDecimal dsr = calculateDsr(activeLoans, request.getRequestedAmount(), appliedRate,
                request.getRequestedPeriod());

        if (dsr.compareTo(DSR_LIMIT) > 0) {
            LoanLedger saved = saveLoanLedger(loanNo, customer.getId(), account.getId(), request,
                    creditScore, dsr, BigDecimal.ZERO, appliedRate, "REJECTED",
                    "DSR 초과 (" + dsr + "%)");
            return LoanEvaluateResponse.rejected(saved.getId(), loanNo,
                    creditScore, dsr, "DSR 초과 (" + dsr + "%)");
        }

        // BK-B16: 대출 한도 산출
        BigDecimal approvedLimit = calculateApprovedLimit(activeLoans, appliedRate,
                request.getRequestedPeriod()).min(request.getRequestedAmount());

        // BK-B18: 추천 상품 필터링 및 정렬
        List<AvailableProductDto> products = loanProductMapper
                .findMatchingProducts(approvedLimit, appliedRate)
                .stream().map(AvailableProductDto::from).toList();

        // BK-B19: 심사 결과 저장 (approvedLimit 함께 저장 — 실행 시 검증·폴링 재계산 방지용)
        LoanLedger saved = saveLoanLedger(loanNo, customer.getId(), account.getId(), request,
                creditScore, dsr, approvedLimit, appliedRate, "APPROVED", null);

        return LoanEvaluateResponse.approved(saved.getId(), loanNo,
                approvedLimit, appliedRate, creditScore, dsr, products);
    }

    // ── BK-B20: 심사 상태 Polling ─────────────────────────────────────────────

    @Transactional(readOnly = true)
    public LoanEvaluationStatusResponse getEvaluationStatus(Long evaluationId) {
        LoanLedger ledger = loanLedgerMapper.findById(evaluationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.LOAN_NOT_FOUND));

        // approvedLimit은 심사 시점에 저장된 값을 그대로 반환 (재계산 금지)
        return LoanEvaluationStatusResponse.from(ledger, ledger.getApprovedLimit());
    }

    // ── BK-B21 ~ B24, B27: 대출 실행 ────────────────────────────────────────

    @Transactional
    public LoanExecuteResponse executeLoan(LoanExecuteRequest request) {
        LoanLedger loanLedger = loanLedgerMapper.findByLoanNo(request.getLoanNo())
                .orElseThrow(() -> new BusinessException(ErrorCode.LOAN_NOT_FOUND));

        if ("ACTIVE".equals(loanLedger.getStatus())) {
            throw new BusinessException(ErrorCode.LOAN_ALREADY_EXECUTED);
        }
        if (!"APPROVED".equals(loanLedger.getStatus())) {
            throw new BusinessException(ErrorCode.LOAN_INVALID_STATUS);
        }

        LoanProduct product = loanProductMapper.findByProductId(request.getProductId())
                .orElseThrow(() -> new BusinessException(ErrorCode.LOAN_PRODUCT_NOT_FOUND));

        // 실행 금액이 심사 시점 승인 한도를 초과하는지 검증
        if (request.getLoanAmount().compareTo(loanLedger.getApprovedLimit()) > 0) {
            throw new BusinessException(ErrorCode.LOAN_EXCEED_APPROVED_LIMIT);
        }

        // BK-B24: 불완전판매 방지 — 동일 상품 60일 내 재실행 금지
        if (loanLedgerMapper.existsRecentActiveByCustomerAndProduct(
                loanLedger.getCustomerId(), product.getProductId())) {
            throw new BusinessException(ErrorCode.LOAN_INCOMPLETE_SALE_PREVENTION);
        }

        // BK-B21: 계좌 유효성 검증 (비관적 락)
        Account account = accountMapper.findByIdForUpdate(loanLedger.getLinkedAccountId())
                .orElseThrow(() -> new BusinessException(ErrorCode.LOAN_ACCOUNT_NOT_FOUND));

        if (!"NORMAL".equals(account.getStatus())) {
            throw new BusinessException(ErrorCode.LOAN_ACCOUNT_ABNORMAL);
        }

        // BK-B22: 계좌 비밀번호 검증 (bcrypt)
        if (!passwordEncoder.matches(request.getAccountPassword(), account.getPassword())) {
            throw new BusinessException(ErrorCode.LOAN_ACCOUNT_PASSWORD_MISMATCH);
        }

        Customer customer = customerMapper.findById(account.getCustomerId())
                .orElseThrow(() -> new BusinessException(ErrorCode.LOAN_CUSTOMER_NOT_FOUND));

        BigDecimal interestRate = loanLedger.getInterestRate();
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = startDate.plusMonths(request.getRepaymentPeriod());

        // BK-B27: 월 상환금액 계산
        BigDecimal monthlyPayment = calculateMonthlyPayment(
                request.getLoanAmount(), interestRate, request.getRepaymentPeriod());

        // BK-B23: 단일 트랜잭션 — 대출 원장 갱신 + 잔액 증가 + 거래 기록
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

        accountMapper.updateBalance(account.getId(), request.getLoanAmount());

        BigDecimal balanceAfter = account.getBalance().add(request.getLoanAmount());
        String txId = "LOAN-" + UUID.randomUUID().toString().replace("-", "").toUpperCase();
        transactionLedgerMapper.insert(TransactionLedger.of(
                txId, account.getId(), "LOAN", request.getLoanAmount(),
                balanceAfter, product.getProductName() + " 대출 실행", "SUCCESS"));

        return LoanExecuteResponse.builder()
                .loanNo(request.getLoanNo())
                .customerName(customer.getCustomerName())
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

    // ── 기존 상품 목록 조회 ───────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<LoanProductResponse> getActiveProducts() {
        return loanProductMapper.findAllActive().stream()
                .map(LoanProductResponse::from)
                .toList();
    }

    // ── private helpers ──────────────────────────────────────────────────────

    private LoanLedger saveLoanLedger(String loanNo, Long customerId, Long accountId,
            LoanEvaluateRequest req, int creditScore, BigDecimal dsr,
            BigDecimal approvedLimit, BigDecimal rate, String status, String rejectReason) {
        LoanLedger ledger = LoanLedger.builder()
                .loanNo(loanNo)
                .customerId(customerId)
                .linkedAccountId(accountId)
                .requestedAmount(req.getRequestedAmount())
                .requestedPeriod(req.getRequestedPeriod())
                .isCreditInfoAgreed(req.getIsCreditInfoAgreed())
                .isProductTermsAgreed(req.getIsProductTermsAgreed())
                .isDocumentCollected(req.getIsDocumentCollected())
                .appliedCreditScore(creditScore)
                .appliedDsr(dsr)
                .approvedLimit(approvedLimit)
                .interestRate(rate)
                .rejectReason(rejectReason)
                .reviewedAt(LocalDateTime.now())
                .status(status)
                .build();
        loanLedgerMapper.insert(ledger);
        return ledger;
    }

    // BK-B13: NICE 신용정보원 연계 Mock (실제 연동 전 stub)
    private int getMockCreditScore(Long customerId) {
        return 750;
    }

    // BK-B17: 신용점수 기반 금리 산출
    private BigDecimal calculateAppliedRate(int creditScore) {
        if (creditScore >= 900) return BASE_RATE.add(new BigDecimal("0.50"));
        if (creditScore >= 800) return BASE_RATE.add(new BigDecimal("1.50"));
        if (creditScore >= 700) return BASE_RATE.add(new BigDecimal("2.50"));
        return BASE_RATE.add(new BigDecimal("3.00"));
    }

    // BK-B15: DSR 계산
    private BigDecimal calculateDsr(List<LoanLedger> activeLoans, BigDecimal newAmount,
            BigDecimal rate, int period) {
        BigDecimal existingMonthly = activeLoans.stream()
                .map(l -> calculateMonthlyPayment(
                        l.getLoanAmount() != null ? l.getLoanAmount()
                                : (l.getApprovedLimit() != null ? l.getApprovedLimit() : BigDecimal.ZERO),
                        l.getInterestRate() != null ? l.getInterestRate() : rate,
                        l.getRepaymentPeriod() != null ? l.getRepaymentPeriod() : period))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal newMonthly = calculateMonthlyPayment(newAmount, rate, period);
        BigDecimal annualRepayment = existingMonthly.add(newMonthly).multiply(new BigDecimal("12"));

        return annualRepayment.divide(MOCK_ANNUAL_INCOME, 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"))
                .setScale(2, RoundingMode.HALF_UP);
    }

    // BK-B16: DSR 40% 이내 최대 대출 한도 역산
    private BigDecimal calculateApprovedLimit(List<LoanLedger> activeLoans,
            BigDecimal rate, int period) {
        BigDecimal maxAnnual = MOCK_ANNUAL_INCOME.multiply(new BigDecimal("0.40"));
        BigDecimal existingMonthly = activeLoans.stream()
                .map(l -> calculateMonthlyPayment(
                        l.getLoanAmount() != null ? l.getLoanAmount()
                                : (l.getApprovedLimit() != null ? l.getApprovedLimit() : BigDecimal.ZERO),
                        l.getInterestRate() != null ? l.getInterestRate() : rate,
                        l.getRepaymentPeriod() != null ? l.getRepaymentPeriod() : period))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal availableMonthly = maxAnnual.divide(new BigDecimal("12"), 4, RoundingMode.HALF_UP)
                .subtract(existingMonthly);

        if (availableMonthly.compareTo(BigDecimal.ZERO) <= 0) return BigDecimal.ZERO;

        if (rate.compareTo(BigDecimal.ZERO) == 0) {
            return availableMonthly.multiply(new BigDecimal(period)).setScale(0, RoundingMode.DOWN);
        }

        // 역산: M = P·r(1+r)^n / ((1+r)^n − 1) → P = M·((1+r)^n − 1) / (r·(1+r)^n)
        BigDecimal r = rate.divide(new BigDecimal("1200"), 10, RoundingMode.HALF_UP);
        BigDecimal pow = BigDecimal.ONE.add(r).pow(period, new MathContext(20));
        BigDecimal limit = availableMonthly
                .multiply(pow.subtract(BigDecimal.ONE))
                .divide(r.multiply(pow), 0, RoundingMode.DOWN);

        return limit.min(MAX_LOAN_AMOUNT);
    }

    // BK-B27: 원리금균등상환 월 납입금 계산
    BigDecimal calculateMonthlyPayment(BigDecimal principal, BigDecimal annualRate, int months) {
        if (principal.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;
        if (annualRate.compareTo(BigDecimal.ZERO) == 0) {
            return principal.divide(new BigDecimal(months), 0, RoundingMode.CEILING);
        }
        BigDecimal r = annualRate.divide(new BigDecimal("1200"), 10, RoundingMode.HALF_UP);
        BigDecimal pow = BigDecimal.ONE.add(r).pow(months, new MathContext(20));
        return principal.multiply(r).multiply(pow)
                .divide(pow.subtract(BigDecimal.ONE), 0, RoundingMode.CEILING);
    }

    private String generateLoanNo() {
        return "LN-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
    }
}

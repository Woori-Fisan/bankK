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
import com.woorifisan.bank.domain.loan.dto.response.LoanExecuteResponse;
import com.woorifisan.bank.domain.loan.dto.response.LoanProductResponse;
import com.woorifisan.bank.domain.loan.mapper.LoanLedgerMapper;
import com.woorifisan.bank.domain.loan.mapper.LoanProductMapper;
import com.woorifisan.bank.domain.loan.model.LoanLedger;
import com.woorifisan.bank.domain.loan.model.LoanProduct;
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
    private static final BigDecimal MAX_LEGAL_RATE = new BigDecimal("20.00");
    private static final BigDecimal BASE_RATE = new BigDecimal("3.50");

    private final CustomerMapper customerMapper;
    private final AccountMapper accountMapper;
    private final TransactionLedgerMapper transactionLedgerMapper;
    private final LoanLedgerMapper loanLedgerMapper;
    private final LoanProductMapper loanProductMapper;
    private final BCryptPasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public List<LoanProductResponse> getActiveProducts() {
        return loanProductMapper.findAllActive().stream()
                .map(LoanProductResponse::from)
                .toList();
    }

    @Transactional
    public LoanEvaluateResponse evaluateLoan(LoanEvaluateRequest request) {
        Account account = accountMapper.findByAccountNoPlain(request.getDepositAccountNo())
                .orElseThrow(() -> new BusinessException(ErrorCode.LOAN_ACCOUNT_NOT_FOUND));

        if (!"NORMAL".equals(account.getStatus())) {
            throw new BusinessException(ErrorCode.LOAN_ACCOUNT_ABNORMAL);
        }

        Customer customer = customerMapper.findById(account.getCustomerId())
                .orElseThrow(() -> new BusinessException(ErrorCode.LOAN_CUSTOMER_NOT_FOUND));

        String loanNo = generateLoanNo();
        int creditScore = getMockCreditScore(customer.getId());

        if (creditScore < 600) {
            saveLoanLedger(loanNo, customer.getId(), account.getId(), request,
                    creditScore, BigDecimal.ZERO, null, null, "REJECTED", "신용점수 미달 (" + creditScore + "점)");
            return LoanEvaluateResponse.rejected(loanNo, creditScore, BigDecimal.ZERO, "신용점수 미달 (" + creditScore + "점)");
        }

        BigDecimal appliedRate = calculateAppliedRate(creditScore);

        List<LoanLedger> activeLoans = loanLedgerMapper.findActiveByCustomerId(customer.getId());
        BigDecimal dsr = calculateDsr(activeLoans, request.getRequestedAmount(), appliedRate, request.getRequestedPeriod());

        if (dsr.compareTo(DSR_LIMIT) > 0) {
            saveLoanLedger(loanNo, customer.getId(), account.getId(), request,
                    creditScore, dsr, appliedRate, null, "REJECTED", "DSR 초과 (" + dsr + "%)");
            return LoanEvaluateResponse.rejected(loanNo, creditScore, dsr, "DSR 초과 (" + dsr + "%)");
        }

        BigDecimal approvedLimit = calculateApprovedLimit(activeLoans, appliedRate, request.getRequestedPeriod())
                .min(request.getRequestedAmount());

        List<AvailableProductDto> products = loanProductMapper.findMatchingProducts(approvedLimit, appliedRate)
                .stream().map(AvailableProductDto::from).toList();

        saveLoanLedger(loanNo, customer.getId(), account.getId(), request,
                creditScore, dsr, appliedRate, null, "APPROVED", null);

        return LoanEvaluateResponse.approved(loanNo, approvedLimit, appliedRate, creditScore, dsr, products);
    }

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

        Account account = accountMapper.findByIdForUpdate(loanLedger.getLinkedAccountId())
                .orElseThrow(() -> new BusinessException(ErrorCode.LOAN_ACCOUNT_NOT_FOUND));

        if (!"NORMAL".equals(account.getStatus())) {
            throw new BusinessException(ErrorCode.LOAN_ACCOUNT_ABNORMAL);
        }

        if (!passwordEncoder.matches(request.getAccountPassword(), account.getPasswordHash())) {
            throw new BusinessException(ErrorCode.LOAN_ACCOUNT_PASSWORD_MISMATCH);
        }

        BigDecimal interestRate = loanLedger.getInterestRate();
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = startDate.plusMonths(request.getRepaymentPeriod());
        BigDecimal monthlyPayment = calculateMonthlyPayment(
                request.getLoanAmount(), interestRate, request.getRepaymentPeriod());

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
        String txId = "LOAN-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
        transactionLedgerMapper.insert(TransactionLedger.of(
                txId, account.getId(), "LOAN", request.getLoanAmount(),
                balanceAfter, product.getProductName() + " 대출 실행", "SUCCESS"));

        return LoanExecuteResponse.builder()
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

    // ── private helpers ──────────────────────────────────────────────────────

    private void saveLoanLedger(String loanNo, Long customerId, Long accountId,
            LoanEvaluateRequest req, int creditScore, BigDecimal dsr,
            BigDecimal rate, BigDecimal approvedLimit, String status, String rejectReason) {
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
                .interestRate(rate)
                .rejectReason(rejectReason)
                .reviewedAt(LocalDateTime.now())
                .status(status)
                .build();
        loanLedgerMapper.insert(ledger);
    }

    private int getMockCreditScore(Long customerId) {
        return 750;
    }

    private BigDecimal calculateAppliedRate(int creditScore) {
        if (creditScore >= 800) return BASE_RATE.add(new BigDecimal("1.50"));
        return BASE_RATE.add(new BigDecimal("3.00"));
    }

    private BigDecimal calculateDsr(List<LoanLedger> activeLoans, BigDecimal newAmount,
            BigDecimal rate, int period) {
        BigDecimal existingMonthly = activeLoans.stream()
                .map(l -> calculateMonthlyPayment(
                        l.getLoanAmount() != null ? l.getLoanAmount() : BigDecimal.ZERO,
                        l.getInterestRate() != null ? l.getInterestRate() : rate,
                        l.getRepaymentPeriod() != null ? l.getRepaymentPeriod() : period))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal newMonthly = calculateMonthlyPayment(newAmount, rate, period);
        BigDecimal annualRepayment = existingMonthly.add(newMonthly)
                .multiply(new BigDecimal("12"));

        return annualRepayment.divide(MOCK_ANNUAL_INCOME, 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateApprovedLimit(List<LoanLedger> activeLoans,
            BigDecimal rate, int period) {
        BigDecimal maxAnnual = MOCK_ANNUAL_INCOME.multiply(new BigDecimal("0.40"));
        BigDecimal existingMonthly = activeLoans.stream()
                .map(l -> calculateMonthlyPayment(
                        l.getLoanAmount() != null ? l.getLoanAmount() : BigDecimal.ZERO,
                        l.getInterestRate() != null ? l.getInterestRate() : rate,
                        l.getRepaymentPeriod() != null ? l.getRepaymentPeriod() : period))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal availableMonthly = maxAnnual.divide(new BigDecimal("12"), 4, RoundingMode.HALF_UP)
                .subtract(existingMonthly);

        if (availableMonthly.compareTo(BigDecimal.ZERO) <= 0) return BigDecimal.ZERO;

        // 역산: M = P * r(1+r)^n / ((1+r)^n-1)  →  P = M * ((1+r)^n-1) / (r*(1+r)^n)
        if (rate.compareTo(BigDecimal.ZERO) == 0) {
            return availableMonthly.multiply(new BigDecimal(period)).setScale(0, RoundingMode.DOWN);
        }
        BigDecimal r = rate.divide(new BigDecimal("1200"), 10, RoundingMode.HALF_UP);
        BigDecimal pow = BigDecimal.ONE.add(r).pow(period, new MathContext(20));
        BigDecimal limit = availableMonthly
                .multiply(pow.subtract(BigDecimal.ONE))
                .divide(r.multiply(pow), 0, RoundingMode.DOWN);

        return limit.min(new BigDecimal("100000000"));
    }

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

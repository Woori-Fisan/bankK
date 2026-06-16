package com.woorifisan.bank.domain.loan.service;

import com.woorifisan.bank.domain.account.mapper.AccountMapper;
import com.woorifisan.bank.domain.customer.mapper.CustomerMapper;
import com.woorifisan.bank.domain.customer.model.Customer;
import com.woorifisan.bank.domain.loan.dto.response.AvailableProductDto;
import com.woorifisan.bank.domain.loan.dto.response.SensitiveLoanEvaluateResponse;
import com.woorifisan.bank.domain.loan.mapper.LoanLedgerMapper;
import com.woorifisan.bank.domain.loan.mapper.LoanProductMapper;
import com.woorifisan.bank.domain.loan.model.LoanLedger;
import com.woorifisan.bank.domain.loan.model.LoanProduct;
import com.woorifisan.bank.global.security.service.SecurityService;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.crypto.SecretKey;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoanReviewAsyncService {

    // 모든 고객의 연소득을 3,600만 원으로 고정 (Mock)
    private static final BigDecimal MOCK_ANNUAL_INCOME = new BigDecimal("36000000");
    // DSR 한도 40% — 연 소득 대비 연간 원리금 상환액 비율
    private static final BigDecimal DSR_LIMIT = new BigDecimal("40");
    // 기본 금리. 신용점수에 따라 가산금리가 붙는다
    private static final BigDecimal BASE_RATE = new BigDecimal("3.50");
    // 신용점수 최저 기준. 미달 시 즉시 거절
    private static final int CREDIT_SCORE_MIN = 600;
    private static final BigDecimal MAX_LOAN_AMOUNT = new BigDecimal("100000000");
    // webhook 전송 실패 시 최대 재시도 횟수
    private static final int WEBHOOK_MAX_RETRY = 3;

    @Value("${platform.callback.url}")
    private String platformCallbackUrl;

    @Value("${platform.webhook.secret}")
    private String webhookSecret;

    private final LoanLedgerMapper loanLedgerMapper;
    private final LoanProductMapper loanProductMapper;
    private final CustomerMapper customerMapper;
    private final AccountMapper accountMapper;
    private final RestTemplate restTemplate;
    private final SecurityService securityService;

    // 비동기 심사 메인 — sendWebhook(HTTP 재시도 최대 3회) 중 DB 커넥션 점유를 막기 위해 @Transactional 제거
    // 각 mapper 호출은 트랜잭션 없이 auto-commit으로 처리됨 (단일 쿼리라 원자성 유지)
    @Async("loanReviewExecutor")
    public void processReview(String loanNo, String requestKey, SecretKey cek) {
        log.info("[심사] 비동기 심사 시작 - loanNo: {}", loanNo);
        try {
            Thread.sleep(3000); // 데모용 딜레이 — "심사 중입니다" 화면이 보이도록
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
        }
        LoanLedger ledger = loanLedgerMapper.findByLoanNo(loanNo).orElse(null);
        if (ledger == null) {
            log.error("[심사] loan_ledger 조회 실패 - loanNo: {}", loanNo);
            return;
        }

        try {
            Customer customer = customerMapper.findById(ledger.getCustomerId())
                    .orElseThrow(() -> new IllegalStateException("고객 정보 없음"));

            // 1단계: 신용점수 조회 (현재 Mock — 실제 신용조회 기관 API 연동 시 교체)
            int creditScore = getMockCreditScore(customer.getId());

            // 2단계: 신용점수 컷. 600 미만이면 즉시 거절
            if (creditScore < CREDIT_SCORE_MIN) {
                String reason = "신용점수 미달 (" + creditScore + "점)";
                updateRejected(loanNo, creditScore, BigDecimal.ZERO, BigDecimal.ZERO, reason);
                sendWebhook(requestKey, loanNo, "REJECTED", null, null, reason, null, cek);
                return;
            }

            // 3단계: 신용점수 기반 적용금리 계산
            BigDecimal appliedRate = calculateAppliedRate(creditScore);

            // 4단계: 법정최고금리(20%) 초과 시 거절
            if (appliedRate.compareTo(new BigDecimal("20")) > 0) {
                String reason = "법정최고금리 초과 (" + appliedRate + "%)";
                updateRejected(loanNo, creditScore, BigDecimal.ZERO, BigDecimal.ZERO, reason);
                sendWebhook(requestKey, loanNo, "REJECTED", null, null, reason, null, cek);
                return;
            }

            // 5단계: 기존 대출을 포함한 DSR 기반 승인 한도 계산
            List<LoanLedger> activeLoans = loanLedgerMapper.findActiveByCustomerId(ledger.getCustomerId());
            BigDecimal approvedLimit = calculateApprovedLimit(activeLoans, appliedRate, ledger.getRequestedPeriod())
                    .min(ledger.getRequestedAmount());

            // 6단계: 승인 한도가 100만 원 미만이면 거절
            if (approvedLimit.compareTo(new BigDecimal("1000000")) < 0) {
                BigDecimal dsr = calculateDsr(activeLoans, ledger.getRequestedAmount(), appliedRate, ledger.getRequestedPeriod());
                String reason = "DSR 초과 또는 한도 부족 (" + dsr + "%)";
                updateRejected(loanNo, creditScore, dsr, BigDecimal.ZERO, reason);
                sendWebhook(requestKey, loanNo, "REJECTED", null, null, reason, null, cek);
                return;
            }

            BigDecimal dsr = calculateDsr(activeLoans, approvedLimit, appliedRate, ledger.getRequestedPeriod());

            // 7단계: 승인 한도와 적용금리 조건에 맞는 상품 조회
            List<AvailableProductDto> products = loanProductMapper
                    .findMatchingProducts(approvedLimit, appliedRate)
                    .stream().map(p -> AvailableProductDto.from(p, calculateProductRate(p, creditScore))).toList();

            // 8단계: 심사 결과 APPROVED 로 저장
            LoanLedger forUpdate = LoanLedger.builder()
                    .loanNo(loanNo)
                    .appliedCreditScore(creditScore)
                    .appliedDsr(dsr)
                    .approvedLimit(approvedLimit)
                    .interestRate(appliedRate)
                    .reviewedAt(LocalDateTime.now())
                    .status("APPROVED")
                    .build();
            loanLedgerMapper.updateReviewResult(forUpdate);

            log.info("[심사] 심사 완료 APPROVED - loanNo: {}, limit: {}", loanNo, approvedLimit);
            // 9단계: Platform 에 webhook 전송 → SSE 로 프론트에 결과 전달
            sendWebhook(requestKey, loanNo, "APPROVED", approvedLimit, appliedRate, null, products, cek);

        } catch (Exception e) {
            // 예상치 못한 오류 발생 시 SYSTEM_ERROR 로 저장하고 webhook 전송
            log.error("[심사] 심사 중 오류 발생 - loanNo: {}", loanNo, e);
            LoanLedger forError = LoanLedger.builder()
                    .loanNo(loanNo)
                    .reviewedAt(LocalDateTime.now())
                    .status("SYSTEM_ERROR")
                    .rejectReason("시스템 내부 오류")
                    .build();
            loanLedgerMapper.updateReviewResult(forError);
            sendWebhook(requestKey, loanNo, "SYSTEM_ERROR", null, null, "시스템 내부 오류", null, cek);
        }
    }

    // 거절 결과를 loan_ledger 에 저장하는 공통 헬퍼
    private void updateRejected(String loanNo, int creditScore, BigDecimal dsr,
            BigDecimal rate, String reason) {
        LoanLedger forUpdate = LoanLedger.builder()
                .loanNo(loanNo)
                .appliedCreditScore(creditScore)
                .appliedDsr(dsr)
                .approvedLimit(BigDecimal.ZERO)
                .interestRate(rate)
                .rejectReason(reason)
                .reviewedAt(LocalDateTime.now())
                .status("REJECTED")
                .build();
        loanLedgerMapper.updateReviewResult(forUpdate);
    }

    // Webhook 전송
    private void sendWebhook(String requestKey, String loanNo, String status,
            BigDecimal approvedLimit, BigDecimal interestRate,
            String rejectReason, List<AvailableProductDto> products, SecretKey cek) {
        String url = platformCallbackUrl + "/api/v1/loan/callback";

        // 민감 정보 암호화 (resPayload 생성)
        SensitiveLoanEvaluateResponse sensitive = SensitiveLoanEvaluateResponse.builder()
                .approvedLimit(approvedLimit)
                .interestRate(interestRate)
                .rejectReason(rejectReason)
                .availableProducts(products)
                .build();
        String resPayload = securityService.encryptResponse(sensitive, cek);

        // Platform이 세션 식별을 위해 필요한 필드는 평문으로 유지
        Map<String, Object> body = new HashMap<>();
        body.put("requestKey", requestKey);
        body.put("loanNo", loanNo);
        body.put("status", status);
        body.put("resPayload", resPayload);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        // Platform 이 이 헤더 값으로 위조 요청 여부를 검증한다
        headers.set("X-Webhook-Secret", webhookSecret);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        // 최대 3회 재시도. 성공하면 즉시 return
        for (int attempt = 1; attempt <= WEBHOOK_MAX_RETRY; attempt++) {
            try {
                restTemplate.postForEntity(url, entity, Void.class);
                log.info("[Webhook] 전송 성공 - requestKey: {}, status: {}", requestKey, status);
                return;
            } catch (Exception e) {
                log.warn("[Webhook] 전송 실패 {}/{} - requestKey: {}", attempt, WEBHOOK_MAX_RETRY, requestKey, e);
            }
        }

        // 3회 모두 실패 시 WEBHOOK_FAILED 기록. 이 건은 SSE 타임아웃으로 사용자에게 안내된다.
        log.error("[Webhook] 최종 실패 - loanNo: {} 상태 WEBHOOK_FAILED 기록", loanNo);
        LoanLedger forFailed = LoanLedger.builder()
                .loanNo(loanNo)
                .status("WEBHOOK_FAILED")
                .build();
        loanLedgerMapper.updateReviewResult(forFailed);
    }

    // 심사 계산 로직
    // Mock: 실제 신용조회 기관 API 연동 전까지 750점 고정
    private int getMockCreditScore(Long customerId) { return 750; }

    // 신용점수 구간별 가산금리. 점수가 높을수록 금리가 낮아진다.
    private BigDecimal calculateAppliedRate(int creditScore) {
        if (creditScore >= 900) return BASE_RATE.add(new BigDecimal("0.50"));
        if (creditScore >= 800) return BASE_RATE.add(new BigDecimal("1.50"));
        if (creditScore >= 700) return BASE_RATE.add(new BigDecimal("2.50"));
        return BASE_RATE.add(new BigDecimal("3.00"));
    }

    // DSR(%) = (기존 월납입금 합계 + 신규 월납입금) × 12 / 연소득 × 100
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
                .multiply(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP);
    }

    // DSR 40% 한도 내에서 원리금균등상환 역산 공식으로 최대 대출 가능 금액 계산
    // 공식: P = availableMonthly × ((1+r)^n - 1) / (r × (1+r)^n)  (r=월금리, n=기간)
    private BigDecimal calculateApprovedLimit(List<LoanLedger> activeLoans, BigDecimal rate, int period) {
        BigDecimal maxAnnual = MOCK_ANNUAL_INCOME.multiply(new BigDecimal("0.40")); // 연소득의 40%
        BigDecimal existingMonthly = activeLoans.stream()
                .map(l -> calculateMonthlyPayment(
                        l.getLoanAmount() != null ? l.getLoanAmount()
                                : (l.getApprovedLimit() != null ? l.getApprovedLimit() : BigDecimal.ZERO),
                        l.getInterestRate() != null ? l.getInterestRate() : rate,
                        l.getRepaymentPeriod() != null ? l.getRepaymentPeriod() : period))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        // DSR 한도에서 기존 월납입금을 뺀 나머지가 신규 대출에 쓸 수 있는 월납입 여유분
        BigDecimal availableMonthly = maxAnnual.divide(new BigDecimal("12"), 4, RoundingMode.HALF_UP)
                .subtract(existingMonthly);
        if (availableMonthly.compareTo(BigDecimal.ZERO) <= 0) return BigDecimal.ZERO;
        if (rate.compareTo(BigDecimal.ZERO) == 0) {
            return availableMonthly.multiply(new BigDecimal(period)).setScale(0, RoundingMode.DOWN);
        }
        BigDecimal r = rate.divide(new BigDecimal("1200"), 10, RoundingMode.HALF_UP); // 연금리 → 월금리
        BigDecimal pow = BigDecimal.ONE.add(r).pow(period, new MathContext(20));
        return availableMonthly.multiply(pow.subtract(BigDecimal.ONE))
                .divide(r.multiply(pow), 0, RoundingMode.DOWN)
                .min(MAX_LOAN_AMOUNT); // 최대 1억 캡
    }

    // 신용점수 → 상품 금리 범위 내 보간 공식: productRate = minRate + (maxRate - minRate) × factor
    // factor: 900+→0.00, 800~899→0.25, 700~799→0.50, 600~699→0.75
    // creditScore null 이면 가장 높은 factor(0.75) 적용, 상품 금리 필드 null 이면 BASE_RATE 반환
    BigDecimal calculateProductRate(LoanProduct product, Integer creditScore) {
        BigDecimal minRate = (product != null && product.getMinRate() != null)
                ? product.getMinRate() : BASE_RATE;
        BigDecimal maxRate = (product != null && product.getMaxRate() != null)
                ? product.getMaxRate() : BASE_RATE;

        BigDecimal factor;
        if (creditScore == null)          factor = new BigDecimal("0.75");
        else if (creditScore >= 900)      factor = BigDecimal.ZERO;
        else if (creditScore >= 800)      factor = new BigDecimal("0.25");
        else if (creditScore >= 700)      factor = new BigDecimal("0.50");
        else                              factor = new BigDecimal("0.75");

        BigDecimal range = maxRate.subtract(minRate);
        return minRate.add(range.multiply(factor)).setScale(2, RoundingMode.HALF_UP);
    }

    // 원리금균등상환 월납입금 공식: M = P × r(1+r)^n / ((1+r)^n - 1)
    BigDecimal calculateMonthlyPayment(BigDecimal principal, BigDecimal annualRate, int months) {
        if (principal.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;
        if (annualRate.compareTo(BigDecimal.ZERO) == 0) {
            // 무이자인 경우 원금 균등 분할
            return principal.divide(new BigDecimal(months), 0, RoundingMode.CEILING);
        }
        BigDecimal r = annualRate.divide(new BigDecimal("1200"), 10, RoundingMode.HALF_UP);
        BigDecimal pow = BigDecimal.ONE.add(r).pow(months, new MathContext(20));
        return principal.multiply(r).multiply(pow)
                .divide(pow.subtract(BigDecimal.ONE), 0, RoundingMode.CEILING);
    }
}

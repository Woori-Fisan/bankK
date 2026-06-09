package com.woorifisan.bank.domain.loan.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.woorifisan.bank.domain.account.mapper.AccountMapper;
import com.woorifisan.bank.domain.customer.mapper.CustomerMapper;
import com.woorifisan.bank.domain.customer.model.Customer;
import com.woorifisan.bank.domain.loan.mapper.LoanLedgerMapper;
import com.woorifisan.bank.domain.loan.mapper.LoanProductMapper;
import com.woorifisan.bank.domain.loan.model.LoanLedger;
import com.woorifisan.bank.global.security.service.SecurityService;
import java.util.Optional;
import javax.crypto.SecretKey;
import org.mockito.ArgumentCaptor;
import org.springframework.http.ResponseEntity;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

@ExtendWith(MockitoExtension.class)
class LoanReviewAsyncServiceTest {

    @InjectMocks
    private LoanReviewAsyncService asyncService;

    @Mock private LoanLedgerMapper loanLedgerMapper;
    @Mock private LoanProductMapper loanProductMapper;
    @Mock private CustomerMapper customerMapper;
    @Mock private AccountMapper accountMapper;
    @Mock private RestTemplate restTemplate;
    @Mock private SecurityService securityService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(asyncService, "platformCallbackUrl", "http://localhost:8080");
        ReflectionTestUtils.setField(asyncService, "webhookSecret", "test-secret");
    }

    // ─────────────────────────────────────────────────────────────
    // 원리금균등상환 월납입금 계산 (package-private: 직접 호출)
    // ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("calculateMonthlyPayment - 원리금균등상환 월납입금")
    class CalculateMonthlyPayment {

        @Test
        @DisplayName("원금 0원 → 월납입금 0원")
        void 원금제로() {
            BigDecimal result = asyncService.calculateMonthlyPayment(
                    BigDecimal.ZERO, new BigDecimal("6.00"), 60);

            assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("무이자(0%) 대출 → 원금 균등분할")
        void 무이자() {
            // 1,200,000 / 12 = 100,000 (ceiling)
            BigDecimal result = asyncService.calculateMonthlyPayment(
                    new BigDecimal("1200000"), BigDecimal.ZERO, 12);

            assertThat(result).isEqualByComparingTo(new BigDecimal("100000"));
        }

        @Test
        @DisplayName("연 6% / 60개월 → 시나리오 2 기준값(약 120만원/월) 검증")
        void 연6퍼_60개월() {
            // approvedLimit ≈ 62,034,000 → monthly ≈ 1,200,000
            BigDecimal result = asyncService.calculateMonthlyPayment(
                    new BigDecimal("62034000"), new BigDecimal("6.00"), 60);

            // 연소득 3,600만, DSR 40% = 월 120만 한도이므로 ±2만원 범위 내
            assertThat(result).isBetween(new BigDecimal("1180000"), new BigDecimal("1220000"));
        }
    }

    // ─────────────────────────────────────────────────────────────
    // DSR 40% 한도 기반 승인한도 계산 (private: reflection)
    // ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("calculateApprovedLimit - DSR 40% 한도 기반 승인 한도 (시나리오 2, 6)")
    class CalculateApprovedLimit {

        @Test
        @DisplayName("기존 대출 없음, 연소득 3,600만, 연 6%/60개월 → 약 6,200만원 (시나리오 2 기준값)")
        void 기존대출_없음() {
            BigDecimal limit = invokeCalculateApprovedLimit(List.of(), new BigDecimal("6.00"), 60);

            // 사용자 시나리오 예상값: ~62,034,000. 정수(원) 단위 이므로 ±500,000 허용
            assertThat(limit).isBetween(new BigDecimal("61500000"), new BigDecimal("62500000"));
        }

        @Test
        @DisplayName("기존 ACTIVE 대출 있으면 한도 감소 (시나리오 6 - DSR 증가)")
        void 기존대출_있으면_한도감소() {
            BigDecimal limitNoLoans = invokeCalculateApprovedLimit(List.of(), new BigDecimal("6.00"), 60);

            // 기존 대출: 원금 1,000만 / 6% / 60개월 → 월납 ~19만
            LoanLedger existing = LoanLedger.builder()
                    .loanAmount(new BigDecimal("10000000"))
                    .interestRate(new BigDecimal("6.00"))
                    .repaymentPeriod(60)
                    .build();
            BigDecimal limitWithLoan = invokeCalculateApprovedLimit(
                    List.of(existing), new BigDecimal("6.00"), 60);

            assertThat(limitWithLoan).isLessThan(limitNoLoans);
        }

        @Test
        @DisplayName("기존 대출이 DSR 한도를 가득 채운 경우 → 승인 한도 0원 (시나리오 6 - 거절)")
        void 기존대출로_DSR한도_초과() {
            // 원금 6,300만 / 6% / 60개월 → 월납 ~122만 > DSR한도(120만) → 여유 없음
            LoanLedger bigExistingLoan = LoanLedger.builder()
                    .loanAmount(new BigDecimal("63000000"))
                    .interestRate(new BigDecimal("6.00"))
                    .repaymentPeriod(60)
                    .build();

            BigDecimal limit = invokeCalculateApprovedLimit(
                    List.of(bigExistingLoan), new BigDecimal("6.00"), 60);

            assertThat(limit).isEqualByComparingTo(BigDecimal.ZERO);
        }
    }

    // ─────────────────────────────────────────────────────────────
    // DSR 계산 (private: reflection)
    // ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("calculateDsr - 총부채원리금상환비율 계산")
    class CalculateDsr {

        @Test
        @DisplayName("기존 대출 없을 때 신규 대출 6,200만/6%/60개월 → DSR 40% 미만")
        void 기존대출_없음() {
            BigDecimal dsr = invokeCalculateDsr(
                    List.of(),
                    new BigDecimal("62000000"),
                    new BigDecimal("6.00"),
                    60);

            assertThat(dsr).isLessThan(new BigDecimal("40.00"));
        }

        @Test
        @DisplayName("기존 대출 포함 시 DSR 증가")
        void 기존대출_포함시_DSR증가() {
            BigDecimal dsrBase = invokeCalculateDsr(
                    List.of(), new BigDecimal("30000000"), new BigDecimal("6.00"), 60);

            LoanLedger existing = LoanLedger.builder()
                    .loanAmount(new BigDecimal("10000000"))
                    .interestRate(new BigDecimal("6.00"))
                    .repaymentPeriod(60)
                    .build();
            BigDecimal dsrWithLoan = invokeCalculateDsr(
                    List.of(existing), new BigDecimal("30000000"), new BigDecimal("6.00"), 60);

            assertThat(dsrWithLoan).isGreaterThan(dsrBase);
        }
    }

    // ─────────────────────────────────────────────────────────────
    // 신용점수 기반 가산금리 (private: reflection)
    // ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("calculateAppliedRate - 신용점수 구간별 가산금리")
    class CalculateAppliedRate {

        @Test
        @DisplayName("750점 (Mock 고정값) → 기본금리 3.50 + 가산 2.50 = 6.00%")
        void 점수750() {
            BigDecimal rate = invokeCalculateAppliedRate(750);
            assertThat(rate).isEqualByComparingTo(new BigDecimal("6.00"));
        }

        @Test
        @DisplayName("800점 이상 → 기본금리 3.50 + 가산 1.50 = 5.00%")
        void 점수800() {
            BigDecimal rate = invokeCalculateAppliedRate(800);
            assertThat(rate).isEqualByComparingTo(new BigDecimal("5.00"));
        }

        @Test
        @DisplayName("900점 이상 → 기본금리 3.50 + 가산 0.50 = 4.00%")
        void 점수900() {
            BigDecimal rate = invokeCalculateAppliedRate(900);
            assertThat(rate).isEqualByComparingTo(new BigDecimal("4.00"));
        }

        @Test
        @DisplayName("600점 미만 → 기본금리 3.50 + 가산 3.00 = 6.50%")
        void 점수600미만() {
            BigDecimal rate = invokeCalculateAppliedRate(599);
            assertThat(rate).isEqualByComparingTo(new BigDecimal("6.50"));
        }
    }

    // ─────────────────────────────────────────────────────────────
    // processReview — 비동기 심사 메인 로직
    // 주의: Thread.sleep(3000) 포함 → 테스트 1개당 최소 3초 소요
    // ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("processReview - 비동기 심사 전체 흐름")
    class ProcessReview {

        private static final String LOAN_NO     = "LN-REVIEW001";
        private static final String REQUEST_KEY = "req-review-key";

        private LoanLedger submittedLedger() {
            return LoanLedger.builder()
                    .loanNo(LOAN_NO)
                    .customerId(10L)
                    .requestedAmount(new BigDecimal("30000000"))
                    .requestedPeriod(36)
                    .status("SUBMITTED")
                    .build();
        }

        @Test
        @Timeout(10)
        @DisplayName("loan_ledger 조회 실패 시 즉시 반환 (updateReviewResult 호출 없음)")
        void ledger_없음_즉시반환() {
            given(loanLedgerMapper.findByLoanNo(LOAN_NO)).willReturn(Optional.empty());

            asyncService.processReview(LOAN_NO, REQUEST_KEY, mock(SecretKey.class));

            verify(loanLedgerMapper, never()).updateReviewResult(any());
        }

        @Test
        @Timeout(10)
        @DisplayName("정상 승인 → APPROVED 저장 및 Webhook 1회 전송")
        void 승인_성공() {
            given(loanLedgerMapper.findByLoanNo(LOAN_NO)).willReturn(Optional.of(submittedLedger()));
            given(customerMapper.findById(10L)).willReturn(
                    Optional.of(Customer.builder().id(10L).build()));
            given(loanLedgerMapper.findActiveByCustomerId(10L)).willReturn(List.of());
            given(loanProductMapper.findMatchingProducts(any(), any())).willReturn(List.of());
            given(securityService.encryptResponse(any(), any())).willReturn("enc");
            given(restTemplate.postForEntity(anyString(), any(), any()))
                    .willReturn(ResponseEntity.ok(null));

            asyncService.processReview(LOAN_NO, REQUEST_KEY, mock(SecretKey.class));

            ArgumentCaptor<LoanLedger> captor = ArgumentCaptor.forClass(LoanLedger.class);
            verify(loanLedgerMapper).updateReviewResult(captor.capture());
            assertThat(captor.getValue().getStatus()).isEqualTo("APPROVED");
            verify(restTemplate).postForEntity(anyString(), any(), any());
        }

        @Test
        @Timeout(10)
        @DisplayName("기존 대출로 DSR 한도 초과 → REJECTED 저장")
        void DSR_초과_거절() {
            given(loanLedgerMapper.findByLoanNo(LOAN_NO)).willReturn(Optional.of(submittedLedger()));
            given(customerMapper.findById(10L)).willReturn(
                    Optional.of(Customer.builder().id(10L).build()));
            // 원금 6,300만 → 월납 ~122만 → DSR 한도(120만) 초과 → approvedLimit ≈ 0
            LoanLedger bigLoan = LoanLedger.builder()
                    .loanAmount(new BigDecimal("63000000"))
                    .interestRate(new BigDecimal("6.00"))
                    .repaymentPeriod(60)
                    .build();
            given(loanLedgerMapper.findActiveByCustomerId(10L)).willReturn(List.of(bigLoan));
            given(securityService.encryptResponse(any(), any())).willReturn("enc");
            given(restTemplate.postForEntity(anyString(), any(), any()))
                    .willReturn(ResponseEntity.ok(null));

            asyncService.processReview(LOAN_NO, REQUEST_KEY, mock(SecretKey.class));

            ArgumentCaptor<LoanLedger> captor = ArgumentCaptor.forClass(LoanLedger.class);
            verify(loanLedgerMapper).updateReviewResult(captor.capture());
            assertThat(captor.getValue().getStatus()).isEqualTo("REJECTED");
        }

        @Test
        @Timeout(10)
        @DisplayName("고객 조회 실패 시 SYSTEM_ERROR 저장")
        void SYSTEM_ERROR() {
            given(loanLedgerMapper.findByLoanNo(LOAN_NO)).willReturn(Optional.of(submittedLedger()));
            given(customerMapper.findById(10L))
                    .willThrow(new IllegalStateException("고객 정보 없음"));
            given(securityService.encryptResponse(any(), any())).willReturn("enc");
            given(restTemplate.postForEntity(anyString(), any(), any()))
                    .willReturn(ResponseEntity.ok(null));

            asyncService.processReview(LOAN_NO, REQUEST_KEY, mock(SecretKey.class));

            ArgumentCaptor<LoanLedger> captor = ArgumentCaptor.forClass(LoanLedger.class);
            verify(loanLedgerMapper).updateReviewResult(captor.capture());
            assertThat(captor.getValue().getStatus()).isEqualTo("SYSTEM_ERROR");
        }

        @Test
        @Timeout(10)
        @DisplayName("Webhook 3회 전송 실패 → WEBHOOK_FAILED 저장")
        void Webhook_3회_실패_WEBHOOK_FAILED() {
            given(loanLedgerMapper.findByLoanNo(LOAN_NO)).willReturn(Optional.of(submittedLedger()));
            given(customerMapper.findById(10L)).willReturn(
                    Optional.of(Customer.builder().id(10L).build()));
            given(loanLedgerMapper.findActiveByCustomerId(10L)).willReturn(List.of());
            given(loanProductMapper.findMatchingProducts(any(), any())).willReturn(List.of());
            given(securityService.encryptResponse(any(), any())).willReturn("enc");
            given(restTemplate.postForEntity(anyString(), any(), any()))
                    .willThrow(new RuntimeException("connection refused"));

            asyncService.processReview(LOAN_NO, REQUEST_KEY, mock(SecretKey.class));

            // APPROVED 저장 후 Webhook 3회 실패 → WEBHOOK_FAILED 저장
            ArgumentCaptor<LoanLedger> captor = ArgumentCaptor.forClass(LoanLedger.class);
            verify(loanLedgerMapper, times(2)).updateReviewResult(captor.capture());
            assertThat(captor.getAllValues().get(0).getStatus()).isEqualTo("APPROVED");
            assertThat(captor.getAllValues().get(1).getStatus()).isEqualTo("WEBHOOK_FAILED");
            verify(restTemplate, times(3)).postForEntity(anyString(), any(), any());
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Private 메서드 호출 헬퍼 (Java reflection)
    // ─────────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private BigDecimal invokeCalculateApprovedLimit(List<LoanLedger> loans, BigDecimal rate, int period) {
        try {
            Method m = LoanReviewAsyncService.class.getDeclaredMethod(
                    "calculateApprovedLimit", List.class, BigDecimal.class, int.class);
            m.setAccessible(true);
            return (BigDecimal) m.invoke(asyncService, loans, rate, period);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    private BigDecimal invokeCalculateDsr(List<LoanLedger> loans, BigDecimal amount,
                                          BigDecimal rate, int period) {
        try {
            Method m = LoanReviewAsyncService.class.getDeclaredMethod(
                    "calculateDsr", List.class, BigDecimal.class, BigDecimal.class, int.class);
            m.setAccessible(true);
            return (BigDecimal) m.invoke(asyncService, loans, amount, rate, period);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    private BigDecimal invokeCalculateAppliedRate(int creditScore) {
        try {
            Method m = LoanReviewAsyncService.class.getDeclaredMethod(
                    "calculateAppliedRate", int.class);
            m.setAccessible(true);
            return (BigDecimal) m.invoke(asyncService, creditScore);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

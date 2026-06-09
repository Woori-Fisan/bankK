package com.woorifisan.bank.domain.loan.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.woorifisan.bank.domain.account.mapper.AccountMapper;
import com.woorifisan.bank.domain.account.mapper.TransactionLedgerMapper;
import com.woorifisan.bank.domain.account.model.Account;
import com.woorifisan.bank.domain.customer.mapper.CustomerMapper;
import com.woorifisan.bank.domain.customer.model.Customer;
import com.woorifisan.bank.domain.document.mapper.CommonDocumentMapper;
import com.woorifisan.bank.domain.loan.dto.decrypted.DecryptedLoanEvaluateRequest;
import com.woorifisan.bank.domain.loan.dto.decrypted.DecryptedLoanExecuteRequest;
import com.woorifisan.bank.domain.loan.dto.request.LoanEvaluateRequest;
import com.woorifisan.bank.domain.loan.dto.request.LoanExecuteRequest;
import com.woorifisan.bank.domain.loan.dto.response.LoanExecuteResponse;
import com.woorifisan.bank.domain.loan.mapper.LoanLedgerMapper;
import com.woorifisan.bank.domain.loan.mapper.LoanProductMapper;
import com.woorifisan.bank.domain.loan.model.LoanLedger;
import com.woorifisan.bank.domain.loan.model.LoanProduct;
import com.woorifisan.bank.domain.terms.mapper.BankTermsMapper;
import com.woorifisan.bank.global.exception.BusinessException;
import com.woorifisan.bank.global.response.ErrorCode;
import com.woorifisan.bank.global.security.service.SecurityService;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

@ExtendWith(MockitoExtension.class)
class LoanServiceTest {

    @InjectMocks private LoanService loanService;

    @Mock private SecurityService securityService;
    @Mock private AccountMapper accountMapper;
    @Mock private CustomerMapper customerMapper;
    @Mock private LoanLedgerMapper loanLedgerMapper;
    @Mock private LoanProductMapper loanProductMapper;
    @Mock private BankTermsMapper bankTermsMapper;
    @Mock private CommonDocumentMapper commonDocumentMapper;
    @Mock private BCryptPasswordEncoder passwordEncoder;
    @Mock private LoanReviewAsyncService loanReviewAsyncService;
    @Mock private TransactionLedgerMapper transactionLedgerMapper;

    @TempDir
    Path tempDir;

    private static final String BANK_CODE     = "020";
    private static final String ACCOUNT_NO    = "110-123-456789";
    private static final String RRN_PREFIX    = "9001011";
    private static final String CUSTOMER_NAME = "홍길동";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(loanService, "bankCode", BANK_CODE);
        ReflectionTestUtils.setField(loanService, "documentStoragePath", tempDir.toString());
    }

    // ─────────────────────────────────────────────────────────────
    // 헬퍼
    // ─────────────────────────────────────────────────────────────

    private void mockEvaluateDecrypt(String rrnPrefix, String customerName) {
        DecryptedLoanEvaluateRequest data = new DecryptedLoanEvaluateRequest();
        ReflectionTestUtils.setField(data, "depositAccountNo", ACCOUNT_NO);
        ReflectionTestUtils.setField(data, "customerRrnPrefix", rrnPrefix);
        ReflectionTestUtils.setField(data, "customerName", customerName);
        SecurityService.DecryptionResult<DecryptedLoanEvaluateRequest> result =
                new SecurityService.DecryptionResult<>(data, mock(SecretKey.class));
        given(securityService.decryptWithKey(any(), eq(DecryptedLoanEvaluateRequest.class)))
                .willReturn(result);
    }

    private void mockExecuteDecrypt(String accountNo, String password) {
        DecryptedLoanExecuteRequest data = new DecryptedLoanExecuteRequest();
        ReflectionTestUtils.setField(data, "depositAccountNo", accountNo);
        ReflectionTestUtils.setField(data, "accountPassword", password);
        SecurityService.DecryptionResult<DecryptedLoanExecuteRequest> result =
                new SecurityService.DecryptionResult<>(data, mock(SecretKey.class));
        given(securityService.decryptWithKey(any(), eq(DecryptedLoanExecuteRequest.class)))
                .willReturn(result);
    }

    private LoanEvaluateRequest defaultEvaluateRequest() {
        return LoanEvaluateRequest.builder()
                .reqPayload("dummy-jwe")
                .bankKeyId("test-key")
                .depositBankCode(BANK_CODE)
                .requestedAmount(new BigDecimal("30000000"))
                .requestedPeriod(36)
                .isCreditInfoAgreed(true)
                .isProductTermsAgreed(true)
                .isDocumentCollected(true)
                .requestKey("test-request-key")
                .build();
    }

    // validateFileNames 통과를 위한 최소 조건: 4가지 필수 서류 키워드 포함
    private List<MultipartFile> validFiles() {
        return List.<MultipartFile>of(
                new MockMultipartFile("files", "신분증_사본.pdf", "application/pdf", "d".getBytes()),
                new MockMultipartFile("files", "재직증명서.pdf", "application/pdf", "d".getBytes()),
                new MockMultipartFile("files", "원천징수영수증.pdf", "application/pdf", "d".getBytes()),
                new MockMultipartFile("files", "건강보험료_납부확인서.pdf", "application/pdf", "d".getBytes())
        );
    }

    // ─────────────────────────────────────────────────────────────
    // acceptLoan
    // ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("대출 접수 (acceptLoan)")
    class AcceptLoan {

        @Test
        @DisplayName("실패 - 입금 은행 불일치 → LOAN_DEPOSIT_BANK_MISMATCH (시나리오 1c)")
        void 실패_입금은행_불일치() {
            mockEvaluateDecrypt(RRN_PREFIX, CUSTOMER_NAME);
            LoanEvaluateRequest request = LoanEvaluateRequest.builder()
                    .reqPayload("dummy-jwe").bankKeyId("test-key")
                    .depositBankCode("999")
                    .requestedAmount(new BigDecimal("30000000")).requestedPeriod(36)
                    .isCreditInfoAgreed(true).isProductTermsAgreed(true).isDocumentCollected(true)
                    .requestKey("test-request-key")
                    .build();

            assertThatThrownBy(() -> loanService.acceptLoan(request, validFiles()))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.LOAN_DEPOSIT_BANK_MISMATCH);
        }

        @Test
        @DisplayName("실패 - 입금 계좌 미존재 → LOAN_ACCOUNT_NOT_FOUND")
        void 실패_입금계좌_미존재() {
            mockEvaluateDecrypt(RRN_PREFIX, CUSTOMER_NAME);
            given(accountMapper.findByAccountNoPlain(ACCOUNT_NO)).willReturn(Optional.empty());

            assertThatThrownBy(() -> loanService.acceptLoan(defaultEvaluateRequest(), validFiles()))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.LOAN_ACCOUNT_NOT_FOUND);
        }

        @Test
        @DisplayName("실패 - 계좌 상태 LOCKED → LOAN_ACCOUNT_LOCKED")
        void 실패_계좌상태_LOCKED() {
            mockEvaluateDecrypt(RRN_PREFIX, CUSTOMER_NAME);
            Account locked = Account.builder()
                    .id(1L).customerId(10L).accountNo(ACCOUNT_NO).status("LOCKED").build();
            given(accountMapper.findByAccountNoPlain(ACCOUNT_NO)).willReturn(Optional.of(locked));

            assertThatThrownBy(() -> loanService.acceptLoan(defaultEvaluateRequest(), validFiles()))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.LOAN_ACCOUNT_LOCKED);
        }

        @Test
        @DisplayName("실패 - 고객 이름 불일치 → LOAN_CUSTOMER_IDENTITY_MISMATCH (시나리오 1a)")
        void 실패_이름_불일치() {
            mockEvaluateDecrypt(RRN_PREFIX, "김오류");      // 이름 불일치
            Account normal = Account.builder()
                    .id(1L).customerId(10L).accountNo(ACCOUNT_NO).status("NORMAL").build();
            Customer customer = Customer.builder()
                    .id(10L).rrnPrefix(RRN_PREFIX).customerName(CUSTOMER_NAME).build();
            given(accountMapper.findByAccountNoPlain(ACCOUNT_NO)).willReturn(Optional.of(normal));
            given(customerMapper.findById(10L)).willReturn(Optional.of(customer));

            assertThatThrownBy(() -> loanService.acceptLoan(defaultEvaluateRequest(), validFiles()))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.LOAN_CUSTOMER_IDENTITY_MISMATCH);
        }

        @Test
        @DisplayName("실패 - 주민번호 앞자리 불일치 → LOAN_CUSTOMER_IDENTITY_MISMATCH (시나리오 1b)")
        void 실패_주민번호_불일치() {
            mockEvaluateDecrypt("9001012", CUSTOMER_NAME);  // RRN 불일치
            Account normal = Account.builder()
                    .id(1L).customerId(10L).accountNo(ACCOUNT_NO).status("NORMAL").build();
            Customer customer = Customer.builder()
                    .id(10L).rrnPrefix(RRN_PREFIX).customerName(CUSTOMER_NAME).build();
            given(accountMapper.findByAccountNoPlain(ACCOUNT_NO)).willReturn(Optional.of(normal));
            given(customerMapper.findById(10L)).willReturn(Optional.of(customer));

            assertThatThrownBy(() -> loanService.acceptLoan(defaultEvaluateRequest(), validFiles()))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.LOAN_CUSTOMER_IDENTITY_MISMATCH);
        }

        @Test
        @DisplayName("실패 - 이미 접수 중인 건 존재 → LOAN_DUPLICATE_PENDING")
        void 실패_중복신청() {
            mockEvaluateDecrypt(RRN_PREFIX, CUSTOMER_NAME);
            Account normal = Account.builder()
                    .id(1L).customerId(10L).accountNo(ACCOUNT_NO).status("NORMAL").build();
            Customer customer = Customer.builder()
                    .id(10L).rrnPrefix(RRN_PREFIX).customerName(CUSTOMER_NAME).build();
            given(accountMapper.findByAccountNoPlain(ACCOUNT_NO)).willReturn(Optional.of(normal));
            given(customerMapper.findById(10L)).willReturn(Optional.of(customer));
            given(loanLedgerMapper.existsPendingByCustomerId(10L)).willReturn(true);

            assertThatThrownBy(() -> loanService.acceptLoan(defaultEvaluateRequest(), validFiles()))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.LOAN_DUPLICATE_PENDING);
        }
    }

    // ─────────────────────────────────────────────────────────────
    // executeLoan
    // ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("대출 실행 (executeLoan)")
    class ExecuteLoan {

        private static final String LOAN_NO   = "LN-TEST001";
        private static final String HASHED_PW = "$2a$10$hashedPasswordValue";

        private LoanExecuteRequest defaultExecuteRequest() {
            return LoanExecuteRequest.builder()
                    .reqPayload("dummy-jwe")
                    .bankKeyId("test-key")
                    .loanNo(LOAN_NO)
                    .productId(1L)
                    .loanAmount(new BigDecimal("30000000"))
                    .repaymentPeriod(36)
                    .repaymentType("원리금균등")
                    .build();
        }

        private LoanLedger approvedLedger(BigDecimal approvedLimit) {
            return LoanLedger.builder()
                    .loanNo(LOAN_NO)
                    .status("APPROVED")
                    .approvedLimit(approvedLimit)
                    .interestRate(new BigDecimal("6.00"))
                    .customerId(10L)
                    .linkedAccountId(1L)
                    .build();
        }

        private LoanProduct testProduct() {
            return LoanProduct.builder().productId(1L).productName("테스트상품").build();
        }

        @Test
        @DisplayName("실패 - 이미 ACTIVE 상태 → LOAN_ALREADY_EXECUTED")
        void 실패_이미_실행됨() {
            mockExecuteDecrypt(ACCOUNT_NO, "1234");
            given(loanLedgerMapper.findByLoanNo(LOAN_NO)).willReturn(
                    Optional.of(LoanLedger.builder().loanNo(LOAN_NO).status("ACTIVE").build()));

            assertThatThrownBy(() -> loanService.executeLoan(defaultExecuteRequest()))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.LOAN_ALREADY_EXECUTED);
        }

        @Test
        @DisplayName("실패 - APPROVED 아닌 상태 → LOAN_INVALID_STATUS")
        void 실패_승인되지_않은_상태() {
            mockExecuteDecrypt(ACCOUNT_NO, "1234");
            given(loanLedgerMapper.findByLoanNo(LOAN_NO)).willReturn(
                    Optional.of(LoanLedger.builder().loanNo(LOAN_NO).status("SUBMITTED").build()));

            assertThatThrownBy(() -> loanService.executeLoan(defaultExecuteRequest()))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.LOAN_INVALID_STATUS);
        }

        @Test
        @DisplayName("실패 - 실행 금액이 승인 한도 초과 → LOAN_EXCEED_APPROVED_LIMIT (시나리오 3)")
        void 실패_승인한도_초과() {
            mockExecuteDecrypt(ACCOUNT_NO, "1234");
            // 한도 2천만, 요청 3천만
            given(loanLedgerMapper.findByLoanNo(LOAN_NO))
                    .willReturn(Optional.of(approvedLedger(new BigDecimal("20000000"))));
            given(loanProductMapper.findByProductId(1L)).willReturn(Optional.of(testProduct()));

            assertThatThrownBy(() -> loanService.executeLoan(defaultExecuteRequest()))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.LOAN_EXCEED_APPROVED_LIMIT);
        }

        @Test
        @DisplayName("실패 - 계좌 비밀번호 불일치 → LOAN_ACCOUNT_PASSWORD_MISMATCH (시나리오 5a)")
        void 실패_비밀번호_불일치() {
            mockExecuteDecrypt(ACCOUNT_NO, "9999");
            given(loanLedgerMapper.findByLoanNo(LOAN_NO))
                    .willReturn(Optional.of(approvedLedger(new BigDecimal("50000000"))));
            given(loanProductMapper.findByProductId(1L)).willReturn(Optional.of(testProduct()));
            given(loanLedgerMapper.existsRecentActiveByCustomerAndProduct(10L, 1L)).willReturn(false);
            Account account = Account.builder()
                    .id(1L).customerId(10L).accountNo(ACCOUNT_NO)
                    .password(HASHED_PW).status("NORMAL").version(1).balance(BigDecimal.ZERO).build();
            given(accountMapper.findByIdForUpdate(1L)).willReturn(Optional.of(account));
            given(passwordEncoder.matches("9999", HASHED_PW)).willReturn(false);

            assertThatThrownBy(() -> loanService.executeLoan(defaultExecuteRequest()))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.LOAN_ACCOUNT_PASSWORD_MISMATCH);
        }

        @Test
        @DisplayName("성공 - 비밀번호 일치 및 모든 조건 충족 (시나리오 5b)")
        void 성공() {
            mockExecuteDecrypt(ACCOUNT_NO, "1234");
            given(loanLedgerMapper.findByLoanNo(LOAN_NO))
                    .willReturn(Optional.of(approvedLedger(new BigDecimal("50000000"))));
            given(loanProductMapper.findByProductId(1L)).willReturn(Optional.of(testProduct()));
            given(loanLedgerMapper.existsRecentActiveByCustomerAndProduct(10L, 1L)).willReturn(false);
            Account account = Account.builder()
                    .id(1L).customerId(10L).accountNo(ACCOUNT_NO)
                    .password(HASHED_PW).status("NORMAL").version(1).balance(new BigDecimal("1000000")).build();
            given(accountMapper.findByIdForUpdate(1L)).willReturn(Optional.of(account));
            given(passwordEncoder.matches("1234", HASHED_PW)).willReturn(true);
            given(accountMapper.updateBalance(eq(1L), any(BigDecimal.class), eq(1))).willReturn(1);
            given(customerMapper.findById(10L)).willReturn(
                    Optional.of(Customer.builder().id(10L).customerName(CUSTOMER_NAME).build()));
            given(loanReviewAsyncService.calculateMonthlyPayment(
                    any(BigDecimal.class), any(BigDecimal.class), anyInt()))
                    .willReturn(new BigDecimal("897000"));
            given(securityService.encryptResponse(any(), any())).willReturn("encrypted-payload");

            LoanExecuteResponse response = loanService.executeLoan(defaultExecuteRequest());

            assertThat(response.getLoanNo()).isEqualTo(LOAN_NO);
            assertThat(response.getLoanAmount()).isEqualByComparingTo(new BigDecimal("30000000"));
            assertThat(response.getInterestRate()).isEqualByComparingTo(new BigDecimal("6.00"));
            assertThat(response.getRepaymentPeriod()).isEqualTo(36);
            assertThat(response.getMonthlyPayment()).isEqualByComparingTo(new BigDecimal("897000"));
            assertThat(response.getStartDate()).isNotNull();
        }
    }
}

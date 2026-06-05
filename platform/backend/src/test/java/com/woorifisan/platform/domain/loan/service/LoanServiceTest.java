package com.woorifisan.platform.domain.loan.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.woorifisan.platform.domain.bank.external.client.BankLoanClient;
import com.woorifisan.platform.domain.bank.external.dto.BankLoanEvaluateRequest;
import com.woorifisan.platform.domain.bank.external.dto.BankLoanExecuteRequest;
import com.woorifisan.platform.domain.bank.mapper.BankMapper;
import com.woorifisan.platform.domain.bank.model.Bank;
import com.woorifisan.platform.domain.loan.dto.request.LoanCallbackRequest;
import com.woorifisan.platform.domain.loan.dto.request.LoanEvaluateRequest;
import com.woorifisan.platform.domain.loan.dto.request.LoanExecuteRequest;
import com.woorifisan.platform.domain.loan.dto.response.LoanContractDocumentsResponse;
import com.woorifisan.platform.domain.loan.dto.response.LoanDocumentDto;
import com.woorifisan.platform.domain.loan.dto.response.LoanEvaluateResponse;
import com.woorifisan.platform.domain.loan.dto.response.LoanEvaluationResultResponse;
import com.woorifisan.platform.domain.loan.dto.response.LoanExecuteResponse;
import com.woorifisan.platform.domain.loan.dto.response.LoanRequiredDocumentsResponse;
import com.woorifisan.platform.global.exception.BusinessException;
import com.woorifisan.platform.global.response.ErrorCode;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@ExtendWith(MockitoExtension.class)
class LoanServiceTest {

    @Mock private BankLoanClient bankLoanClient;
    @Mock private BankMapper bankMapper;
    @Mock private RedisTemplate<String, String> redisTemplate;
    @Mock private ValueOperations<String, String> valueOps;
    @Spy  private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private LoanService loanService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(loanService, "webhookSecret", "test-secret");
    }

    @AfterEach
    void tearDown() throws Exception {
        getPendingEmitters().clear();
    }

    @SuppressWarnings("unchecked")
    private ConcurrentHashMap<String, SseEmitter> getPendingEmitters() throws Exception {
        Field field = LoanService.class.getDeclaredField("pendingEmitters");
        field.setAccessible(true);
        return (ConcurrentHashMap<String, SseEmitter>) field.get(loanService);
    }

    // ─────────────────────────────────────────────────────────────────────
    // subscribe
    // ─────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("SSE 구독 성공 - SseEmitter 반환 및 pendingEmitters에 등록")
    void SSE_구독_성공() throws Exception {
        SseEmitter emitter = loanService.subscribe("req-key-1");

        assertThat(emitter).isNotNull();
        assertThat(getPendingEmitters()).containsKey("req-key-1");
    }

    // ─────────────────────────────────────────────────────────────────────
    // getRequiredDocuments
    // ─────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("심사 서류 조회 성공 - Bank API 응답을 TermsDocumentDto 리스트로 변환")
    void 심사_서류_조회_성공() {
        Map<String, Object> termsMap = new HashMap<>();
        termsMap.put("termsCode", "CREDIT_INFO_AGREE");
        termsMap.put("title", "신용정보 조회 동의서");
        termsMap.put("termsUrl", "https://example.com/terms");
        termsMap.put("termsContent", "<html>동의서 내용</html>");
        termsMap.put("isMandatory", true);

        when(bankLoanClient.getEvaluationTerms()).thenReturn(List.of(termsMap));

        LoanRequiredDocumentsResponse response = loanService.getRequiredDocuments(1L);

        assertThat(response.getDocuments()).hasSize(1);
        assertThat(response.getDocuments().get(0).getDocumentType()).isEqualTo("CREDIT_INFO_AGREE");
        assertThat(response.getDocuments().get(0).getDocumentName()).isEqualTo("신용정보 조회 동의서");
        assertThat(response.getDocuments().get(0).getIsMandatory()).isTrue();
    }

    @Test
    @DisplayName("심사 서류 조회 - Bank API가 빈 목록 반환 시 빈 리스트 응답")
    void 심사_서류_조회_빈_목록() {
        when(bankLoanClient.getEvaluationTerms()).thenReturn(List.of());

        LoanRequiredDocumentsResponse response = loanService.getRequiredDocuments(1L);

        assertThat(response.getDocuments()).isEmpty();
    }

    // ─────────────────────────────────────────────────────────────────────
    // evaluateLoan
    // ─────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("심사 신청 성공 - 은행으로부터 loanNo와 SUBMITTED 상태 반환")
    void 심사_신청_성공() {
        Bank activeBank = Bank.builder().bankCode("020").bankName("우리은행").isActive(true).build();
        when(bankMapper.findByBankCode("020")).thenReturn(Optional.of(activeBank));
        when(bankLoanClient.submitEvaluation(any(), anyList()))
                .thenReturn(Map.of("loanNo", "LOAN-2026-001", "status", "SUBMITTED"));

        LoanEvaluateResponse response = loanService.evaluateLoan(defaultEvaluateRequest().build(), List.of(), 1L);

        assertThat(response.getLoanNo()).isEqualTo("LOAN-2026-001");
        assertThat(response.getStatus()).isEqualTo("SUBMITTED");
    }

    @Test
    @DisplayName("심사 신청 성공 - 금액/기간 null 시 기본값(1억원/60개월) 적용")
    void 심사_신청_성공_금액기간_null_시_기본값_적용() {
        Bank activeBank = Bank.builder().bankCode("020").bankName("우리은행").isActive(true).build();
        when(bankMapper.findByBankCode("020")).thenReturn(Optional.of(activeBank));
        when(bankLoanClient.submitEvaluation(any(), anyList()))
                .thenReturn(Map.of("loanNo", "LOAN-2026-001", "status", "SUBMITTED"));

        LoanEvaluateRequest request = defaultEvaluateRequest()
                .requestedAmount(null)
                .requestedPeriod(null)
                .build();

        loanService.evaluateLoan(request, List.of(), 1L);

        ArgumentCaptor<BankLoanEvaluateRequest> captor = ArgumentCaptor.forClass(BankLoanEvaluateRequest.class);
        verify(bankLoanClient).submitEvaluation(captor.capture(), anyList());
        assertThat(captor.getValue().getRequestedAmount()).isEqualByComparingTo(new BigDecimal("100000000"));
        assertThat(captor.getValue().getRequestedPeriod()).isEqualTo(60);
    }

    @Test
    @DisplayName("심사 신청 실패 - 존재하지 않는 은행 코드 → BANK_NOT_FOUND")
    void 심사_신청_실패_은행_없음() {
        when(bankMapper.findByBankCode("999")).thenReturn(Optional.empty());

        LoanEvaluateRequest request = defaultEvaluateRequest().bankCode("999").depositBankCode("999").build();

        assertThatThrownBy(() -> loanService.evaluateLoan(request, List.of(), 1L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.BANK_NOT_FOUND);
    }

    @Test
    @DisplayName("심사 신청 실패 - 비활성 은행 → BANK_NOT_FOUND")
    void 심사_신청_실패_비활성_은행() {
        Bank inactiveBank = Bank.builder().bankCode("020").isActive(false).build();
        when(bankMapper.findByBankCode("020")).thenReturn(Optional.of(inactiveBank));

        assertThatThrownBy(() -> loanService.evaluateLoan(defaultEvaluateRequest().build(), List.of(), 1L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.BANK_NOT_FOUND);
    }

    @Test
    @DisplayName("심사 신청 실패 - 입금 은행코드가 심사 은행과 다름 → LOAN_DEPOSIT_BANK_MISMATCH")
    void 심사_신청_실패_입금은행_불일치() {
        Bank activeBank = Bank.builder().bankCode("020").isActive(true).build();
        when(bankMapper.findByBankCode("020")).thenReturn(Optional.of(activeBank));

        LoanEvaluateRequest request = defaultEvaluateRequest().depositBankCode("004").build();

        assertThatThrownBy(() -> loanService.evaluateLoan(request, List.of(), 1L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.LOAN_DEPOSIT_BANK_MISMATCH);
    }

    // ─────────────────────────────────────────────────────────────────────
    // handleCallback
    // ─────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Webhook 콜백 실패 - Secret 불일치 → LOAN_WEBHOOK_SECRET_INVALID")
    void Webhook_콜백_실패_Secret_불일치() {
        LoanCallbackRequest callback = defaultCallback("req-key", "LOAN-001", "APPROVED");

        assertThatThrownBy(() -> loanService.handleCallback(callback, "wrong-secret"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.LOAN_WEBHOOK_SECRET_INVALID);
    }

    @Test
    @DisplayName("Webhook 콜백 - requestKey null 시 조기 반환 (예외 없음)")
    void Webhook_콜백_requestKey_null_조기반환() {
        LoanCallbackRequest callback = new LoanCallbackRequest();
        ReflectionTestUtils.setField(callback, "loanNo", "LOAN-001");

        // requestKey가 null이면 Redis 저장도 하지 않고 바로 return
        loanService.handleCallback(callback, "test-secret");
    }

    @Test
    @DisplayName("Webhook 콜백 성공 - SSE emitter에 결과 전송 및 연결 종료")
    void Webhook_콜백_성공_SSE_전송() throws Exception {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);

        SseEmitter mockEmitter = mock(SseEmitter.class);
        getPendingEmitters().put("req-key-1", mockEmitter);

        LoanCallbackRequest callback = defaultCallback("req-key-1", "LOAN-001", "APPROVED");
        ReflectionTestUtils.setField(callback, "resPayload", "encrypted-result");

        loanService.handleCallback(callback, "test-secret");

        verify(mockEmitter).send(any(SseEmitter.SseEventBuilder.class));
        verify(mockEmitter).complete();
        assertThat(getPendingEmitters()).doesNotContainKey("req-key-1");
    }

    @Test
    @DisplayName("Webhook 콜백 - SSE emitter 없음(만료) 시 Redis에만 저장 후 반환")
    void Webhook_콜백_SSE_emitter_만료() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);

        LoanCallbackRequest callback = defaultCallback("expired-key", "LOAN-001", "APPROVED");

        loanService.handleCallback(callback, "test-secret");

        verify(valueOps).set(eq("loan:result:expired-key"), anyString(), eq(300L), any(TimeUnit.class));
    }

    // ─────────────────────────────────────────────────────────────────────
    // getResult
    // ─────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("심사 결과 조회 성공 - Redis에서 역직렬화하여 반환")
    void 심사_결과_조회_성공() throws Exception {
        String json = objectMapper.writeValueAsString(
                LoanEvaluationResultResponse.builder()
                        .evaluationStatus("APPROVED")
                        .evaluationId("LOAN-001")
                        .build());

        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get("loan:result:req-key")).thenReturn(json);

        LoanEvaluationResultResponse result = loanService.getResult("req-key");

        assertThat(result).isNotNull();
        assertThat(result.getEvaluationStatus()).isEqualTo("APPROVED");
        assertThat(result.getEvaluationId()).isEqualTo("LOAN-001");
    }

    @Test
    @DisplayName("심사 결과 조회 - Redis에 값 없으면 null 반환")
    void 심사_결과_조회_없으면_null() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get("loan:result:req-key")).thenReturn(null);

        assertThat(loanService.getResult("req-key")).isNull();
    }

    // ─────────────────────────────────────────────────────────────────────
    // getContractDocuments
    // ─────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("계약 서류 조회 성공 - Bank API 응답을 TermsDocumentDto 리스트로 변환")
    void 계약_서류_조회_성공() {
        Map<String, Object> termsMap = new HashMap<>();
        termsMap.put("termsCode", "CONTRACT");
        termsMap.put("title", "대출거래약정서");
        termsMap.put("isMandatory", true);

        when(bankLoanClient.getContractTerms("100", "LOAN-001")).thenReturn(List.of(termsMap));

        LoanContractDocumentsResponse response = loanService.getContractDocuments("100", "LOAN-001", 1L);

        assertThat(response.getLoanProductCode()).isEqualTo("100");
        assertThat(response.getDocuments()).hasSize(1);
        assertThat(response.getDocuments().get(0).getDocumentType()).isEqualTo("CONTRACT");
        assertThat(response.getDocuments().get(0).getIsMandatory()).isTrue();
    }

    // ─────────────────────────────────────────────────────────────────────
    // executeLoan
    // ─────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("대출 실행 성공 - Bank 응답을 LoanExecuteResponse로 변환")
    void 대출_실행_성공() {
        Map<String, Object> bankResponse = new HashMap<>();
        bankResponse.put("loanNo", "LOAN-2026-001");
        bankResponse.put("loanAmount", "30000000");
        bankResponse.put("interestRate", "4.5");
        bankResponse.put("repaymentPeriod", "36");
        bankResponse.put("monthlyPayment", "897000");
        bankResponse.put("startDate", "2026-07-15");
        bankResponse.put("endDate", "2029-06-15");

        when(bankLoanClient.executeLoan(any(BankLoanExecuteRequest.class))).thenReturn(bankResponse);

        LoanExecuteResponse response = loanService.executeLoan(defaultExecuteRequest(), 1L);

        assertThat(response.getLoanNo()).isEqualTo("LOAN-2026-001");
        assertThat(response.getExecuteAmount()).isEqualByComparingTo(new BigDecimal("30000000"));
        assertThat(response.getInterestRate()).isEqualByComparingTo(new BigDecimal("4.5"));
        assertThat(response.getRepaymentPeriod()).isEqualTo(36);
        assertThat(response.getStartDate()).isEqualTo("2026-07-15");
        assertThat(response.getMaturityDate()).isEqualTo("2029-06-15");
    }

    // ─────────────────────────────────────────────────────────────────────
    // 공통 픽스처 팩토리
    // ─────────────────────────────────────────────────────────────────────

    private LoanEvaluateRequest.LoanEvaluateRequestBuilder<?, ?> defaultEvaluateRequest() {
        return LoanEvaluateRequest.builder()
                .requestKey("uuid-test-1234")
                .bankCode("020")
                .customerPhone("01012345678")
                .depositBankCode("020")
                .requestedAmount(new BigDecimal("30000000"))
                .requestedPeriod(36)
                .documents(List.of(
                        LoanDocumentDto.builder()
                                .documentType("CREDIT_INFO_AGREE")
                                .agreedAt("2026-01-01T10:00:00")
                                .build()
                ));
    }

    private LoanExecuteRequest defaultExecuteRequest() {
        return LoanExecuteRequest.builder()
                .loanNo("LOAN-2026-001")
                .productId(100L)
                .executeAmount(new BigDecimal("30000000"))
                .repaymentPeriod(36)
                .repaymentType("원리금균등")
                .build();
    }

    private LoanCallbackRequest defaultCallback(String requestKey, String loanNo, String status) {
        LoanCallbackRequest callback = new LoanCallbackRequest();
        ReflectionTestUtils.setField(callback, "requestKey", requestKey);
        ReflectionTestUtils.setField(callback, "loanNo", loanNo);
        ReflectionTestUtils.setField(callback, "status", status);
        return callback;
    }
}
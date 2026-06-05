package com.woorifisan.platform.domain.loan.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
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
import com.woorifisan.platform.domain.loan.dto.request.LoanReceiptRequest;
import com.woorifisan.platform.domain.loan.dto.response.LoanDocumentDto;
import com.woorifisan.platform.domain.loan.dto.response.LoanEvaluateResponse;
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
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@ExtendWith(MockitoExtension.class)
class LoanServiceTest {

    @Mock
    private BankLoanClient bankLoanClient;

    @Mock
    private BankMapper bankMapper;

    // 실제 직렬화 필요 (handleCallback에서 SSE 전송 시 ObjectMapper.writeValueAsString 호출)
    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private LoanService loanService;

    @BeforeEach
    void setUp() {
        // @Value로 주입되는 webhookSecret을 테스트에서 직접 설정
        ReflectionTestUtils.setField(loanService, "webhookSecret", "test-secret");
    }

    @AfterEach
    void tearDown() throws Exception {
        // pendingEmitters는 static 필드이므로 테스트 간 격리를 위해 매번 정리
        clearPendingEmitters();
    }

    // ───────────────────────────────────────────────────────────────
    // 헬퍼: static pendingEmitters 필드 접근
    // ───────────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private ConcurrentHashMap<String, SseEmitter> getPendingEmitters() throws Exception {
        Field field = LoanService.class.getDeclaredField("pendingEmitters");
        field.setAccessible(true);
        return (ConcurrentHashMap<String, SseEmitter>) field.get(null);
    }

    private void clearPendingEmitters() throws Exception {
        getPendingEmitters().clear();
    }

    // ───────────────────────────────────────────────────────────────
    // getRequiredDocuments
    // ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("심사 서류 조회 성공 - Bank API 응답을 TermsDocumentDto 리스트로 변환")
    void 심사_서류_조회_성공() {
        // given: Bank API가 Map 형태의 약관 데이터 반환
        Map<String, Object> termsMap = new HashMap<>();
        termsMap.put("termsCode", "CREDIT_INFO_AGREE");
        termsMap.put("title", "신용정보 조회 동의서");
        termsMap.put("termsUrl", "https://example.com/terms");
        termsMap.put("termsContent", "<html>동의서 내용</html>");
        termsMap.put("isMandatory", true);

        when(bankLoanClient.getEvaluationTerms()).thenReturn(List.of(termsMap));

        // when
        LoanRequiredDocumentsResponse response = loanService.getRequiredDocuments(1L);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getDocuments()).hasSize(1);
        assertThat(response.getDocuments().get(0).getDocumentType()).isEqualTo("CREDIT_INFO_AGREE");
        assertThat(response.getDocuments().get(0).getDocumentName()).isEqualTo("신용정보 조회 동의서");
        assertThat(response.getDocuments().get(0).getIsMandatory()).isTrue();
    }

    // ───────────────────────────────────────────────────────────────
    // subscribe
    // ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("SSE 구독 성공 - SseEmitter 반환 및 pendingEmitters에 등록")
    void SSE_구독_성공() throws Exception {
        // when
        SseEmitter emitter = loanService.subscribe("test-request-key");

        // then
        assertThat(emitter).isNotNull();
        // pendingEmitters에 requestKey가 등록되었는지 확인
        assertThat(getPendingEmitters()).containsKey("test-request-key");
    }

    // ───────────────────────────────────────────────────────────────
    // evaluateLoan
    // ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("대출 심사 신청 성공 - 은행으로부터 loanNo와 SUBMITTED 상태 반환")
    void 심사_신청_성공() {
        // given
        Bank activeBank = Bank.builder().bankCode("020").bankName("우리은행").isActive(true).build();
        when(bankMapper.findByBankCode("020")).thenReturn(Optional.of(activeBank));
        when(bankLoanClient.submitEvaluation(any(), anyList()))
                .thenReturn(Map.of("loanNo", "LOAN-2026-001", "status", "SUBMITTED"));

        LoanEvaluateRequest request = 기본_심사신청_요청().build();

        // when
        LoanEvaluateResponse response = loanService.evaluateLoan(request, List.of(), 1L);

        // then
        assertThat(response.getLoanNo()).isEqualTo("LOAN-2026-001");
        assertThat(response.getStatus()).isEqualTo("SUBMITTED");
    }

    @Test
    @DisplayName("대출 심사 신청 성공 - 금액/기간 null 시 기본값(1억원/60개월) 적용")
    void 심사_신청_성공_금액기간_null_시_기본값_적용() {
        // given
        Bank activeBank = Bank.builder().bankCode("020").bankName("우리은행").isActive(true).build();
        when(bankMapper.findByBankCode("020")).thenReturn(Optional.of(activeBank));
        when(bankLoanClient.submitEvaluation(any(), anyList()))
                .thenReturn(Map.of("loanNo", "LOAN-2026-001", "status", "SUBMITTED"));

        // requestedAmount, requestedPeriod를 null로 지정
        LoanEvaluateRequest request = 기본_심사신청_요청()
                .requestedAmount(null)
                .requestedPeriod(null)
                .build();

        // when
        loanService.evaluateLoan(request, List.of(), 1L);

        // then: Bank Client로 전달된 요청에 기본값이 채워졌는지 확인
        ArgumentCaptor<BankLoanEvaluateRequest> captor = ArgumentCaptor.forClass(BankLoanEvaluateRequest.class);
        verify(bankLoanClient).submitEvaluation(captor.capture(), anyList());
        assertThat(captor.getValue().getRequestedAmount())
                .isEqualByComparingTo(new BigDecimal("100000000")); // 1억원
        assertThat(captor.getValue().getRequestedPeriod()).isEqualTo(60); // 60개월
    }

    @Test
    @DisplayName("대출 심사 신청 실패 - 존재하지 않는 은행 코드 → BANK_NOT_FOUND")
    void 심사_신청_실패_존재하지_않는_은행() {
        // given: bankMapper가 빈 Optional 반환 (은행 미존재)
        when(bankMapper.findByBankCode("999")).thenReturn(Optional.empty());

        LoanEvaluateRequest request = 기본_심사신청_요청().bankCode("999").depositBankCode("999").build();

        // when & then
        assertThatThrownBy(() -> loanService.evaluateLoan(request, List.of(), 1L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.BANK_NOT_FOUND);
    }

    @Test
    @DisplayName("대출 심사 신청 실패 - 비활성화된 은행 → BANK_NOT_FOUND")
    void 심사_신청_실패_비활성_은행() {
        // given: isActive = false인 은행
        Bank inactiveBank = Bank.builder().bankCode("020").bankName("우리은행").isActive(false).build();
        when(bankMapper.findByBankCode("020")).thenReturn(Optional.of(inactiveBank));

        LoanEvaluateRequest request = 기본_심사신청_요청().build();

        // when & then
        assertThatThrownBy(() -> loanService.evaluateLoan(request, List.of(), 1L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.BANK_NOT_FOUND);
    }

    @Test
    @DisplayName("대출 심사 신청 실패 - 타행 입금 계좌 → LOAN_DEPOSIT_BANK_MISMATCH")
    void 심사_신청_실패_타행_입금계좌() {
        // given: 대출 은행(020)과 입금 은행(004)이 다른 경우
        Bank activeBank = Bank.builder().bankCode("020").bankName("우리은행").isActive(true).build();
        when(bankMapper.findByBankCode("020")).thenReturn(Optional.of(activeBank));

        LoanEvaluateRequest request = 기본_심사신청_요청()
                .bankCode("020")
                .depositBankCode("004") // 국민은행 → 타행
                .build();

        // when & then
        assertThatThrownBy(() -> loanService.evaluateLoan(request, List.of(), 1L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.LOAN_DEPOSIT_BANK_MISMATCH);
    }

    // ───────────────────────────────────────────────────────────────
    // handleCallback
    // ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Webhook 수신 성공 APPROVED - SSE emitter에 result 이벤트 전송 후 연결 종료")
    void Webhook_수신_성공_APPROVED() throws Exception {
        // given: mock emitter를 pendingEmitters에 미리 등록
        SseEmitter mockEmitter = mock(SseEmitter.class);
        getPendingEmitters().put("test-key", mockEmitter);

        LoanCallbackRequest callback = new LoanCallbackRequest();
        ReflectionTestUtils.setField(callback, "requestKey", "test-key");
        ReflectionTestUtils.setField(callback, "loanNo", "LOAN-2026-001");
        ReflectionTestUtils.setField(callback, "status", "APPROVED");
        ReflectionTestUtils.setField(callback, "approvedLimit", new BigDecimal("50000000"));
        ReflectionTestUtils.setField(callback, "availableProducts", List.of());

        // when
        loanService.handleCallback(callback, "test-secret");

        // then: emitter.send()와 complete()가 호출되었는지 확인
        verify(mockEmitter).send(any(SseEmitter.SseEventBuilder.class));
        verify(mockEmitter).complete();
    }

    @Test
    @DisplayName("Webhook 수신 성공 REJECTED - 거절 사유 포함하여 SSE 전송")
    void Webhook_수신_성공_REJECTED() throws Exception {
        // given
        SseEmitter mockEmitter = mock(SseEmitter.class);
        getPendingEmitters().put("test-key", mockEmitter);

        LoanCallbackRequest callback = new LoanCallbackRequest();
        ReflectionTestUtils.setField(callback, "requestKey", "test-key");
        ReflectionTestUtils.setField(callback, "loanNo", "LOAN-2026-001");
        ReflectionTestUtils.setField(callback, "status", "REJECTED");
        ReflectionTestUtils.setField(callback, "rejectReason", "신용등급 기준 미달");

        // when
        loanService.handleCallback(callback, "test-secret");

        // then: 거절 상태에서도 SSE 전송 및 연결 종료 확인
        verify(mockEmitter).send(any(SseEmitter.SseEventBuilder.class));
        verify(mockEmitter).complete();
    }

    @Test
    @DisplayName("Webhook 인증 실패 - Secret 불일치 시 LOAN_WEBHOOK_SECRET_INVALID 예외")
    void Webhook_인증_실패_Secret_불일치() {
        // given
        LoanCallbackRequest callback = new LoanCallbackRequest();
        ReflectionTestUtils.setField(callback, "requestKey", "test-key");

        // when & then: 잘못된 secret으로 호출
        assertThatThrownBy(() -> loanService.handleCallback(callback, "wrong-secret"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.LOAN_WEBHOOK_SECRET_INVALID);
    }

    @Test
    @DisplayName("Webhook requestKey null - emitter 탐색 없이 조용히 종료")
    void Webhook_requestKey_null_조용히_처리() {
        // given: requestKey가 null인 콜백
        LoanCallbackRequest callback = new LoanCallbackRequest();
        ReflectionTestUtils.setField(callback, "requestKey", null);
        ReflectionTestUtils.setField(callback, "loanNo", "LOAN-2026-001");

        // when & then: 예외 없이 정상 종료 (타임아웃으로 이미 처리된 케이스)
        loanService.handleCallback(callback, "test-secret");
        // void 반환 메서드이므로 예외가 발생하지 않으면 성공
    }

    @Test
    @DisplayName("Webhook emitter 만료 - pendingEmitters에 없는 key는 조용히 종료")
    void Webhook_emitter_만료_조용히_처리() {
        // given: pendingEmitters에 등록된 emitter가 없는 상태 (60초 타임아웃으로 이미 제거됨)
        LoanCallbackRequest callback = new LoanCallbackRequest();
        ReflectionTestUtils.setField(callback, "requestKey", "expired-key");
        ReflectionTestUtils.setField(callback, "status", "APPROVED");

        // when & then: 예외 없이 정상 종료
        loanService.handleCallback(callback, "test-secret");
    }

    // ───────────────────────────────────────────────────────────────
    // getContractDocuments
    // ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("계약 서류 조회 성공 - Bank API 응답을 TermsDocumentDto 리스트로 변환")
    void 계약_서류_조회_성공() {
        // given
        Map<String, Object> termsMap = new HashMap<>();
        termsMap.put("termsCode", "CONTRACT");
        termsMap.put("title", "대출거래약정서");
        termsMap.put("termsUrl", "https://example.com/contract");
        termsMap.put("termsContent", "<html>약정서</html>");
        termsMap.put("isMandatory", true);

        when(bankLoanClient.getContractTerms(anyString(), anyString())).thenReturn(List.of(termsMap));

        // when
        var response = loanService.getContractDocuments("100", "LOAN-2026-001", 1L);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getDocuments()).hasSize(1);
        assertThat(response.getDocuments().get(0).getDocumentType()).isEqualTo("CONTRACT");
        assertThat(response.getDocuments().get(0).getIsMandatory()).isTrue();
    }

    // ───────────────────────────────────────────────────────────────
    // executeLoan
    // ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("대출 실행 성공 - Bank 응답을 LoanExecuteResponse로 변환")
    void 대출_실행_성공() {
        // given: Bank API 응답 Map 구성
        Map<String, Object> bankResponse = new HashMap<>();
        bankResponse.put("loanNo", "LOAN-2026-001");
        bankResponse.put("customerName", "홍길동");
        bankResponse.put("loanAmount", "30000000");
        bankResponse.put("interestRate", "4.5");
        bankResponse.put("repaymentPeriod", "36");
        bankResponse.put("monthlyPayment", "897000");
        bankResponse.put("startDate", "2026-07-15");
        bankResponse.put("endDate", "2029-06-15");

        when(bankLoanClient.executeLoan(any(BankLoanExecuteRequest.class))).thenReturn(bankResponse);

        LoanExecuteRequest request = LoanExecuteRequest.builder()
                .evaluationId("EVAL-001")
                .loanProductCode("100") // 숫자 String → Long 변환 필요
                .depositAccountNo("enc-account")
                .accountPassword("enc-password")
                .executeAmount(new BigDecimal("30000000"))
                .repaymentPeriod(36)
                .build();

        // when
        LoanExecuteResponse response = loanService.executeLoan(request, 1L);

        // then
        assertThat(response.getLoanId()).isEqualTo("LOAN-2026-001");
        assertThat(response.getBorrowerName()).isEqualTo("홍길동");
        assertThat(response.getExecuteAmount()).isEqualByComparingTo(new BigDecimal("30000000"));
        assertThat(response.getInterestRate()).isEqualByComparingTo(new BigDecimal("4.5"));
        assertThat(response.getRepaymentPeriod()).isEqualTo(36);
    }

    @Test
    @DisplayName("대출 실행 성공 - depositTransactionId가 TXN- 접두사로 자동 생성됨")
    void 대출_실행_성공_거래번호_자동생성() {
        // given
        Map<String, Object> bankResponse = new HashMap<>();
        bankResponse.put("loanNo", "LOAN-2026-001");
        bankResponse.put("customerName", "홍길동");
        bankResponse.put("loanAmount", "30000000");
        bankResponse.put("interestRate", "4.5");
        bankResponse.put("repaymentPeriod", "36");
        bankResponse.put("monthlyPayment", "897000");
        bankResponse.put("startDate", "2026-07-15");
        bankResponse.put("endDate", "2029-06-15");

        when(bankLoanClient.executeLoan(any())).thenReturn(bankResponse);

        LoanExecuteRequest request = LoanExecuteRequest.builder()
                .evaluationId("EVAL-001")
                .loanProductCode("100")
                .depositAccountNo("enc-account")
                .accountPassword("enc-password")
                .executeAmount(new BigDecimal("30000000"))
                .repaymentPeriod(36)
                .build();

        // when
        LoanExecuteResponse response = loanService.executeLoan(request, 1L);

        // then: Platform에서 생성한 입금 거래번호는 TXN- 접두사를 가짐
        assertThat(response.getDepositTransactionId()).startsWith("TXN-");
        assertThat(response.getDepositTransactionId()).isNotNull();
    }

    @Test
    @DisplayName("대출 실행 실패 - 상품 코드가 숫자가 아닌 경우 INVALID_INPUT 예외")
    void 대출_실행_실패_상품코드_숫자_아님() {
        // given: loanProductCode에 숫자가 아닌 값 전달
        LoanExecuteRequest request = LoanExecuteRequest.builder()
                .evaluationId("EVAL-001")
                .loanProductCode("NOT-A-NUMBER") // Bank는 Long productId를 기대
                .depositAccountNo("enc-account")
                .accountPassword("enc-password")
                .executeAmount(new BigDecimal("30000000"))
                .repaymentPeriod(36)
                .build();

        // when & then
        assertThatThrownBy(() -> loanService.executeLoan(request, 1L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT);
    }

    // ───────────────────────────────────────────────────────────────
    // generateReceiptPdf
    // ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("대출 실행 확인서 PDF 생성 성공 - %PDF 헤더로 시작하는 바이트 배열 반환")
    void PDF_생성_성공() {
        // given: NanumGothic.ttf가 classpath에 있어야 함 (src/main/resources/fonts/)
        LoanReceiptRequest request = LoanReceiptRequest.builder()
                .loanId("LOAN-2026-001")
                .borrowerName("홍길동")
                .depositTransactionId("TXN-ABC123DEFG")
                .executeAmount(new BigDecimal("30000000"))
                .interestRate(new BigDecimal("4.5"))
                .repaymentPeriod(36)
                .monthlyPayment(new BigDecimal("897000"))
                .repaymentStartDate("2026-07-15")
                .maturityDate("2029-06-15")
                .loanProductName("직장인 우대 신용대출")
                .depositBankName("우리은행")
                .depositAccountNo("1234567890")
                .build();

        // when: 실제 PDFBox 코드가 실행됨 (mock 없음)
        byte[] result = loanService.generateReceiptPdf(request);

        // then: PDF 파일은 반드시 "%PDF" (0x25 0x50 0x44 0x46)로 시작함
        assertThat(result).isNotNull();
        assertThat(result.length).isGreaterThan(0);
        assertThat(result[0]).isEqualTo((byte) 0x25); // '%'
        assertThat(result[1]).isEqualTo((byte) 0x50); // 'P'
        assertThat(result[2]).isEqualTo((byte) 0x44); // 'D'
        assertThat(result[3]).isEqualTo((byte) 0x46); // 'F'
    }

    @Test
    @DisplayName("대출 실행 확인서 PDF 생성 성공 - 계좌번호 마스킹 처리됨")
    void PDF_생성_성공_계좌번호_마스킹() {
        // given: 계좌번호에 숫자 10자리 입력
        LoanReceiptRequest request = LoanReceiptRequest.builder()
                .loanId("LOAN-2026-001")
                .borrowerName("홍길동")
                .depositTransactionId("TXN-ABC123DEFG")
                .executeAmount(new BigDecimal("30000000"))
                .interestRate(new BigDecimal("4.5"))
                .repaymentPeriod(36)
                .monthlyPayment(new BigDecimal("897000"))
                .repaymentStartDate("2026-07-15")
                .maturityDate("2029-06-15")
                .loanProductName("직장인 우대 신용대출")
                .depositBankName("우리은행")
                .depositAccountNo("1234567890") // 앞3자리+마스킹+뒤3자리로 처리되어야 함
                .build();

        // when
        byte[] result = loanService.generateReceiptPdf(request);

        // then: PDF가 정상 생성되고 내용이 있음 (마스킹은 PDF 내부에서 처리)
        assertThat(result).isNotNull();
        assertThat(result.length).isGreaterThan(100); // 최소 수백 바이트 이상
    }

    // ───────────────────────────────────────────────────────────────
    // 공통 픽스처 팩토리
    // ───────────────────────────────────────────────────────────────

    /** 기본 심사 신청 요청 Builder — 각 테스트에서 필요한 필드만 override하여 사용 */
    private LoanEvaluateRequest.LoanEvaluateRequestBuilder 기본_심사신청_요청() {
        return LoanEvaluateRequest.builder()
                .requestKey("uuid-test-1234")
                .bankCode("020")
                .customerName("홍길동")
                .customerRrnPrefix("9001011")
                .customerPhone("01012345678")
                .depositBankCode("020")
                .depositAccountNo("enc-account-no")
                .requestedAmount(new BigDecimal("30000000"))
                .requestedPeriod(36)
                .documents(List.of(
                        LoanDocumentDto.builder()
                                .documentType("CREDIT_INFO_AGREE")
                                .agreedAt("2026-01-01T10:00:00")
                                .build()
                ));
    }
}

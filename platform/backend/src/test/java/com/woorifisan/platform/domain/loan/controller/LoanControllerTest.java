package com.woorifisan.platform.domain.loan.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.woorifisan.platform.domain.loan.dto.request.LoanEvaluateRequest;
import com.woorifisan.platform.domain.loan.dto.request.LoanExecuteRequest;
import com.woorifisan.platform.domain.loan.dto.response.LoanContractDocumentsResponse;
import com.woorifisan.platform.domain.loan.dto.response.LoanDocumentDto;
import com.woorifisan.platform.domain.loan.dto.response.LoanEvaluateResponse;
import com.woorifisan.platform.domain.loan.dto.response.LoanExecuteResponse;
import com.woorifisan.platform.domain.loan.dto.response.LoanRequiredDocumentsResponse;
import com.woorifisan.platform.domain.loan.dto.response.TermsDocumentDto;
import com.woorifisan.platform.domain.loan.service.LoanService;
import com.woorifisan.platform.global.exception.BusinessException;
import com.woorifisan.platform.global.exception.GlobalExceptionHandler;
import com.woorifisan.platform.global.response.ErrorCode;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(LoanController.class)
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
class LoanControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private LoanService loanService;

    // ───────────────────────────────────────────
    // 1. 심사 서류 조회
    // ───────────────────────────────────────────

    @Test
    @DisplayName("심사 서류 조회 API 성공 - 필수 서류 목록 반환")
    void 심사_서류_조회_성공() throws Exception {
        // given
        TermsDocumentDto doc = TermsDocumentDto.builder()
                .documentType("CREDIT_INFO_AGREE")
                .documentName("신용정보 조회 동의서")
                .documentUrl("https://example.com/terms")
                .isMandatory(true)
                .build();
        LoanRequiredDocumentsResponse response = LoanRequiredDocumentsResponse.builder()
                .documents(List.of(doc))
                .build();

        given(loanService.getRequiredDocuments(any())).willReturn(response);

        // when & then
        mockMvc.perform(get("/api/v1/loan/review/documents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.documents[0].documentType").value("CREDIT_INFO_AGREE"))
                .andExpect(jsonPath("$.data.documents[0].isMandatory").value(true))
                .andDo(print());
    }

    // ───────────────────────────────────────────
    // 2. 대출 심사 신청 (multipart)
    // ───────────────────────────────────────────

    @Test
    @DisplayName("대출 심사 신청 API 성공 - multipart data + 파일 전송")
    void 심사_신청_성공() throws Exception {
        // given
        LoanEvaluateRequest evalRequest = LoanEvaluateRequest.builder()
                .requestKey("uuid-test-1234")
                .bankCode("020")
                .depositBankCode("020")
                .requestedAmount(new BigDecimal("30000000"))
                .requestedPeriod(36)
                .documents(List.of(
                        LoanDocumentDto.builder()
                                .documentType("CREDIT_INFO_AGREE")
                                .agreedAt("2026-01-01T10:00:00")
                                .build()
                ))
                .build();

        LoanEvaluateResponse evalResponse = new LoanEvaluateResponse("LOAN-2026-001", "SUBMITTED");
        given(loanService.evaluateLoan(any(), any(), any())).willReturn(evalResponse);

        // multipart: data 파트(JSON) + files 파트(PDF) 구성
        String dataJson = objectMapper.writeValueAsString(evalRequest);
        MockMultipartFile dataPart = new MockMultipartFile(
                "data", "", MediaType.APPLICATION_JSON_VALUE, dataJson.getBytes(StandardCharsets.UTF_8));
        MockMultipartFile filePart = new MockMultipartFile(
                "files", "신분증.pdf", MediaType.APPLICATION_PDF_VALUE, "PDF content".getBytes());

        // when & then
        mockMvc.perform(multipart("/api/v1/loan/evaluation")
                        .file(dataPart)
                        .file(filePart)
                        .header("x-bank-key-id", "test-key-id"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.loanNo").value("LOAN-2026-001"))
                .andExpect(jsonPath("$.data.status").value("SUBMITTED"))
                .andDo(print());
    }

    @Test
    @DisplayName("대출 심사 신청 API 실패 - 필수 필드 누락 시 400 반환")
    void 심사_신청_실패_유효성검증() throws Exception {
        // given: requestKey, bankCode 등 @NotBlank 필드가 없는 빈 JSON
        MockMultipartFile dataPart = new MockMultipartFile(
                "data", "", MediaType.APPLICATION_JSON_VALUE, "{}".getBytes(StandardCharsets.UTF_8));
        MockMultipartFile filePart = new MockMultipartFile(
                "files", "doc.pdf", MediaType.APPLICATION_PDF_VALUE, "content".getBytes());

        // when & then
        mockMvc.perform(multipart("/api/v1/loan/evaluation")
                        .file(dataPart)
                        .file(filePart))
                .andExpect(status().isBadRequest())
                .andDo(print());
    }

    // ───────────────────────────────────────────
    // 3. 은행 Webhook 콜백
    // ───────────────────────────────────────────

    @Test
    @DisplayName("Webhook 콜백 수신 성공 - 200 OK 빈 바디 반환")
    void Webhook_콜백_수신_성공() throws Exception {
        // given: 서비스는 void 반환 → 별도 given 설정 불필요
        String callbackJson = """
                {
                  "requestKey": "uuid-test-1234",
                  "loanNo": "LOAN-2026-001",
                  "status": "APPROVED",
                  "approvedLimit": 50000000,
                  "availableProducts": []
                }
                """;

        // when & then: X-Webhook-Secret 헤더와 함께 전송
        mockMvc.perform(post("/api/v1/loan/callback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(callbackJson)
                        .header("X-Webhook-Secret", "valid-secret"))
                .andExpect(status().isOk())
                .andDo(print());
    }

    @Test
    @DisplayName("Webhook 콜백 실패 - Secret 불일치 시 401 반환")
    void Webhook_콜백_실패_Secret_불일치() throws Exception {
        // given: 서비스에서 LOAN_WEBHOOK_SECRET_INVALID(401) 예외 발생
        willThrow(new BusinessException(ErrorCode.LOAN_WEBHOOK_SECRET_INVALID))
                .given(loanService).handleCallback(any(), anyString());

        String callbackJson = """
                {"requestKey": "uuid-1234", "loanNo": "LOAN-001", "status": "APPROVED"}
                """;

        // when & then
        mockMvc.perform(post("/api/v1/loan/callback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(callbackJson)
                        .header("X-Webhook-Secret", "wrong-secret"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("LOAN_008"))
                .andDo(print());
    }

    // ───────────────────────────────────────────
    // 4. 계약 서류 조회
    // ───────────────────────────────────────────

    @Test
    @DisplayName("계약 서류 조회 API 성공 - 상품코드와 대출번호로 서류 목록 반환")
    void 계약_서류_조회_성공() throws Exception {
        // given
        TermsDocumentDto doc = TermsDocumentDto.builder()
                .documentType("CONTRACT")
                .documentName("대출거래약정서")
                .isMandatory(true)
                .build();
        LoanContractDocumentsResponse response = LoanContractDocumentsResponse.builder()
                .loanProductCode("100")
                .loanProductName("직장인 우대 신용대출")
                .documents(List.of(doc))
                .build();

        given(loanService.getContractDocuments(anyString(), anyString(), any())).willReturn(response);

        // when & then
        mockMvc.perform(get("/api/v1/loan/contract/documents/{productCode}/{loanNo}",
                        "100", "LOAN-2026-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.loanProductCode").value("100"))
                .andExpect(jsonPath("$.data.documents[0].documentType").value("CONTRACT"))
                .andDo(print());
    }

    // ───────────────────────────────────────────
    // 5. 대출 실행
    // ───────────────────────────────────────────

    @Test
    @DisplayName("대출 실행 API 성공 - 실행 완료 후 대출 정보 반환")
    void 대출_실행_성공() throws Exception {
        // given
        LoanExecuteRequest request = LoanExecuteRequest.builder()
                .loanNo("LOAN-2026-001")
                .productId(1L)
                .executeAmount(new BigDecimal("30000000"))
                .repaymentPeriod(36)
                .repaymentType("원리금균등")
                .build();

        LoanExecuteResponse response = LoanExecuteResponse.builder()
                .loanNo("LOAN-2026-001")
                .executeAmount(new BigDecimal("30000000"))
                .interestRate(new BigDecimal("4.5"))
                .repaymentPeriod(36)
                .monthlyPayment(new BigDecimal("897000"))
                .startDate("2026-07-15")
                .maturityDate("2029-06-15")
                .build();

        given(loanService.executeLoan(any(), any())).willReturn(response);

        // when & then
        mockMvc.perform(post("/api/v1/loan/contract/execution")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("x-bank-key-id", "test-key-id")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.loanNo").value("LOAN-2026-001"))
                .andDo(print());
    }

    @Test
    @DisplayName("대출 실행 API 실패 - 필수 필드 누락 시 400 반환")
    void 대출_실행_실패_유효성검증() throws Exception {
        // given: evaluationId, loanProductCode 등 @NotBlank 필드 없는 빈 요청
        String emptyJson = "{}";

        // when & then
        mockMvc.perform(post("/api/v1/loan/contract/execution")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("x-bank-key-id", "test-key-id")
                        .content(emptyJson))
                .andExpect(status().isBadRequest())
                .andDo(print());
    }

    @Test
    @DisplayName("대출 실행 API 실패 - 은행 API 오류 시 BusinessException 전파")
    void 대출_실행_실패_은행API_오류() throws Exception {
        // given
        LoanExecuteRequest request = LoanExecuteRequest.builder()
                .loanNo("LOAN-2026-001")
                .productId(1L)
                .executeAmount(new BigDecimal("30000000"))
                .repaymentPeriod(36)
                .repaymentType("원리금균등")
                .build();

        given(loanService.executeLoan(any(), any()))
                .willThrow(new BusinessException(ErrorCode.LOAN_BANK_ROUTING_ERROR));

        // when & then
        mockMvc.perform(post("/api/v1/loan/contract/execution")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("x-bank-key-id", "test-key-id")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("LOAN_001"))
                .andDo(print());
    }

}

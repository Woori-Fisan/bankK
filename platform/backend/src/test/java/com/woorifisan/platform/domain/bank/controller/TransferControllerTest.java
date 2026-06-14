package com.woorifisan.platform.domain.bank.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.woorifisan.platform.domain.bank.dto.request.TransferRecipientRequest;
import com.woorifisan.platform.domain.bank.dto.request.TransferRequest;
import com.woorifisan.platform.domain.bank.dto.response.TransferRecipientResponse;
import com.woorifisan.platform.domain.bank.dto.response.TransferResponse;
import com.woorifisan.platform.domain.bank.service.TransferService;
import com.woorifisan.platform.global.exception.BusinessException;
import com.woorifisan.platform.global.exception.GlobalExceptionHandler;
import com.woorifisan.platform.global.idempotency.filter.IdempotencyFilter;
import com.woorifisan.platform.global.response.ErrorCode;
import com.woorifisan.platform.global.security.aspect.TerminalSignatureAspect;
import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(TransferController.class)
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
class TransferControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TerminalSignatureAspect terminalSignatureAspect;

    @MockitoBean
    private IdempotencyFilter idempotencyFilter;

    @MockitoBean
    private TransferService transferService;

    @Test
    @DisplayName("수취인 조회 API 성공 테스트")
    void getRecipient_api_success() throws Exception {
        // given
        TransferRecipientRequest request = TransferRecipientRequest.builder()
                .depositBankCode("020")
                .reqPayload("encrypted-payload")
                .build();

        TransferRecipientResponse response = TransferRecipientResponse.builder()
                .depositBankName("우리은행")
                .accountStatus("NORMAL")
                .resPayload("encrypted-res-payload")
                .build();

        given(transferService.getRecipient(any(), eq("bank-key-id"))).willReturn(response);

        // when & then
        mockMvc.perform(post("/api/v1/bank/transfer/recipient")
                        .header("x-jws-signature", "test-sig")
                        .header("x-bank-key-id", "bank-key-id")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.depositBankName").value("우리은행"))
                .andExpect(jsonPath("$.data.resPayload").value("encrypted-res-payload"))
                .andDo(print());
    }

    @Test
    @DisplayName("수취인 조회 API 유효성 검사 실패 테스트 (입금 은행 코드 누락)")
    void getRecipient_api_validationFailure() throws Exception {
        // given
        TransferRecipientRequest request = TransferRecipientRequest.builder()
                .depositBankCode("") // 공백
                .build();

        // when & then
        mockMvc.perform(post("/api/v1/bank/transfer/recipient")
                        .header("x-jws-signature", "test-sig")
                        .header("x-bank-key-id", "bank-key-id")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andDo(print());
    }

    @Test
    @DisplayName("수취인 조회 API 서비스 예외 테스트 (계좌 미존재)")
    void getRecipient_api_serviceException() throws Exception {
        // given
        TransferRecipientRequest request = TransferRecipientRequest.builder()
                .depositBankCode("020")
                .build();

        given(transferService.getRecipient(any(), eq("bank-key-id")))
                .willThrow(new BusinessException(ErrorCode.INQUIRY_ACCOUNT_NOTFOUND, "존재하지 않는 계좌입니다."));

        // when & then
        mockMvc.perform(post("/api/v1/bank/transfer/recipient")
                        .header("x-jws-signature", "test-sig")
                        .header("x-bank-key-id", "bank-key-id")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.message").value("존재하지 않는 계좌입니다."))
                .andDo(print());
    }

    @Test
    @DisplayName("이체 실행 API 성공 테스트")
    void executeTransfer_api_success() throws Exception {
        // given
        TransferRequest request = new TransferRequest();
        request.setReqPayload("enc-withdraw-payload");
        request.setWithdrawalBankCode("020");
        request.setDepositBankCode("004");
        request.setAmount(new BigDecimal("10000"));

        TransferResponse response = TransferResponse.builder()
                .transactionId("tx-12345")
                .transactionDate("2026-06-10 12:00:00")
                .balanceAfter(new BigDecimal("90000"))
                .resPayload("enc-res-payload")
                .build();

        given(transferService.executeTransfer(any(), eq("w-key")))
                .willReturn(response);

        // when & then
        mockMvc.perform(post("/api/v1/bank/transfer")
                        .header("x-jws-signature", "test-sig")
                        .header("x-bank-key-id", "w-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.transactionId").value("tx-12345"))
                .andExpect(jsonPath("$.data.balanceAfter").value("90000"))
                .andDo(print());
    }

    @Test
    @DisplayName("이체 실행 API 유효성 검사 실패 테스트 (이체 금액 누락)")
    void executeTransfer_api_validationFailure_amountNull() throws Exception {
        // given
        TransferRequest request = new TransferRequest();
        request.setReqPayload("enc-withdraw-payload");
        request.setWithdrawalBankCode("020");
        request.setDepositBankCode("004");
        request.setAmount(null); // 이체 금액 누락

        // when & then
        mockMvc.perform(post("/api/v1/bank/transfer")
                        .header("x-jws-signature", "test-sig")
                        .header("x-bank-key-id", "w-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andDo(print());
    }

    @Test
    @DisplayName("이체 실행 API 유효성 검사 실패 테스트 (이체 금액이 0 이하)")
    void executeTransfer_api_validationFailure_amountNegative() throws Exception {
        // given
        TransferRequest request = new TransferRequest();
        request.setReqPayload("enc-withdraw-payload");
        request.setWithdrawalBankCode("020");
        request.setDepositBankCode("004");
        request.setAmount(BigDecimal.ZERO); // 경계값인 0 이하 검증

        // when & then
        mockMvc.perform(post("/api/v1/bank/transfer")
                        .header("x-jws-signature", "test-sig")
                        .header("x-bank-key-id", "w-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andDo(print());
    }

    @Test
    @DisplayName("이체 실행 API 서비스 예외 테스트 (잔액 부족)")
    void executeTransfer_api_serviceException() throws Exception {
        // given
        TransferRequest request = new TransferRequest();
        request.setReqPayload("enc-withdraw-payload");
        request.setWithdrawalBankCode("020");
        request.setDepositBankCode("004");
        request.setAmount(new BigDecimal("10000"));

        given(transferService.executeTransfer(any(), eq("w-key")))
                .willThrow(new BusinessException(ErrorCode.TRANSFER_WITHDRAW_AMOUNT_FAULT, "잔액이 부족합니다."));

        // when & then
        mockMvc.perform(post("/api/v1/bank/transfer")
                        .header("x-jws-signature", "test-sig")
                        .header("x-bank-key-id", "w-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.message").value("잔액이 부족합니다."))
                .andDo(print());
    }
}

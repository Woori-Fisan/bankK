package com.woorifisan.platform.domain.bank.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.woorifisan.platform.domain.bank.dto.request.WithdrawalRequest;
import com.woorifisan.platform.domain.bank.dto.response.TransferResponse;
import com.woorifisan.platform.domain.bank.service.WithdrawalService;
import com.woorifisan.platform.global.exception.GlobalExceptionHandler;
import com.woorifisan.platform.global.idempotency.filter.IdempotencyFilter;
import com.woorifisan.platform.global.security.aspect.TerminalSignatureAspect;
import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(WithdrawController.class)
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
class WithdrawControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TerminalSignatureAspect terminalSignatureAspect;

    @MockitoBean
    private IdempotencyFilter idempotencyFilter;

    @MockitoBean
    private WithdrawalService withdrawalService;

    @Test
    @DisplayName("출금 실행 API 성공 테스트")
    void executeWithdraw_api_success() throws Exception {
        // given
        WithdrawalRequest request = WithdrawalRequest.builder()
                .reqPayload("encryptedPayload")
                .withdrawalBankCode("020")
                .amount(new BigDecimal("10000"))
                .build();

        TransferResponse response = TransferResponse.builder()
                .transactionId("WD-20230520-001")
                .balanceAfter(new BigDecimal("23456"))
                .transactionDate("2023-05-20 10:00:00")
                .build();

        given(withdrawalService.executeWithdraw(any(WithdrawalRequest.class), anyString())).willReturn(response);

        // when & then
        mockMvc.perform(post("/api/v1/bank/withdrawals")
                        .header("x-jws-signature", "jwsSignature")
                        .header("x-bank-key-id", "test-key-id")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.transactionId").value("WD-20230520-001"))
                .andExpect(jsonPath("$.data.balanceAfter").value(23456))
                .andDo(print());
    }

    @Test
    @DisplayName("출금 실행 API 실패 테스트 - 필수값 누락")
    void executeWithdraw_api_fail_invalid_input() throws Exception {
        // when & then
        mockMvc.perform(post("/api/v1/bank/withdrawals")
                        .header("x-jws-signature", "jwsSignature")
                        .header("x-bank-key-id", "test-key-id")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("ERR_001"))
                .andDo(print());
    }
}

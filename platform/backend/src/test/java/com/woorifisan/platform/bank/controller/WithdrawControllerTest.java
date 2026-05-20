package com.woorifisan.platform.bank.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.woorifisan.platform.domain.bank.controller.WithdrawController;
import com.woorifisan.platform.domain.bank.dto.request.WithdrawalRequest;
import com.woorifisan.platform.domain.bank.dto.response.TransferResponse;
import com.woorifisan.platform.domain.bank.service.WithdrawalService;
import com.woorifisan.platform.global.exception.GlobalExceptionHandler;
import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
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

    @MockBean
    private WithdrawalService withdrawalService;

    @Test
    @DisplayName("출금 실행 API 성공 테스트")
    void executeWithdraw_api_success() throws Exception {
        // given
        WithdrawalRequest request = new WithdrawalRequest();
        request.setEncryptedKey("encryptedKey");
        request.setJwsSignature("jwsSignature");
        request.setWithdrawalBankCode("020");
        request.setWithdrawalAccountNo("1234567890");
        request.setWithdrawalPassword("password");
        request.setCustomerRrnPrefix("900101");
        request.setAmount(new BigDecimal("10000"));

        TransferResponse response = TransferResponse.builder()
                .transactionId("WD-20230520-001")
                .balanceAfter(new BigDecimal("23456"))
                .transactionDate("2023-05-20 10:00:00")
                .build();

        given(withdrawalService.executeWithdraw(any())).willReturn(response);

        // when & then
        mockMvc.perform(post("/api/v1/bank/withdrawals")
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
        // given
        WithdrawalRequest request = new WithdrawalRequest();
        // 필수 필드들을 비워둠

        // when & then
        mockMvc.perform(post("/api/v1/bank/withdrawals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("ERR_001"))
                .andDo(print());
    }
}

package com.woorifisan.platform.domain.bank.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.woorifisan.platform.domain.bank.dto.request.BalanceInquiryRequest;
import com.woorifisan.platform.domain.bank.dto.response.BalanceInquiryResponse;
import com.woorifisan.platform.domain.bank.service.AccountInquiryService;
import com.woorifisan.platform.global.exception.BusinessException;
import com.woorifisan.platform.global.exception.GlobalExceptionHandler;
import com.woorifisan.platform.global.response.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AccountInquiryController.class)
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
class AccountInquiryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AccountInquiryService accountInquiryService;

    @Test
    @DisplayName("잔액 조회 API 성공 테스트")
    void getBalance_api_success() throws Exception {
        // given
        BalanceInquiryRequest request = BalanceInquiryRequest.builder()
                .reqPayload("encryptedPayload")
                .bankCode("001")
                .build();

        BalanceInquiryResponse response = BalanceInquiryResponse.builder()
                .status("NORMAL")
                .build();

        given(accountInquiryService.getBalance(any(), anyString())).willReturn(response);

        // when & then
        mockMvc.perform(post("/api/v1/bank/inquiry/balance")
                        .header("x-bank-key-id", "test-key-id")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("NORMAL"))
                .andDo(print());
    }

    @Test
    @DisplayName("잔액 조회 API 실패 테스트 - 비즈니스 예외 발생")
    void getBalance_api_fail() throws Exception {
        // given
        BalanceInquiryRequest request = BalanceInquiryRequest.builder()
                .reqPayload("encryptedPayload")
                .bankCode("001")
                .build();

        given(accountInquiryService.getBalance(any(), anyString())).willThrow(new BusinessException(ErrorCode.TRANSFER_DEPOSIT_ACCOUNT_FAULT));

        // when & then
        mockMvc.perform(post("/api/v1/bank/inquiry/balance")
                        .header("x-bank-key-id", "test-key-id")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("TRANSFER_002"))
                .andDo(print());
    }
}
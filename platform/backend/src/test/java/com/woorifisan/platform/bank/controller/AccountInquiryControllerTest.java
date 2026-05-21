package com.woorifisan.platform.bank.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.woorifisan.platform.domain.bank.controller.AccountInquiryController;
import com.woorifisan.platform.domain.bank.dto.request.BalanceInquiryRequest;
import com.woorifisan.platform.domain.bank.dto.response.BalanceInquiryResponse;
import com.woorifisan.platform.domain.bank.service.AccountInquiryService;
import com.woorifisan.platform.global.exception.BusinessException;
import com.woorifisan.platform.global.response.ErrorCode;
import com.woorifisan.platform.global.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AccountInquiryController.class)
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
class AccountInquiryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AccountInquiryService accountInquiryService;

    @Test
    @DisplayName("잔액 조회 API 성공 테스트")
    void getBalance_api_success() throws Exception {
        // given
        BalanceInquiryRequest request = BalanceInquiryRequest.builder()
                .accountNo("12345")
                .bankCode("001")
                .customerRrnPrefix("9501011")
                .encryptedKey("key")
                .jwsSignature("sig")
                .build();

        BalanceInquiryResponse response = BalanceInquiryResponse.builder()
                .balance(new BigDecimal("50000"))
                .status("NORMAL")
                .build();

        given(accountInquiryService.getBalance(any())).willReturn(response);

        // when & then
        mockMvc.perform(post("/api/v1/bank/inquiry/balance")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.balance").value("50000"))
                .andDo(print());
    }

    @Test
    @DisplayName("잔액 조회 API 실패 테스트 - 비즈니스 예외 발생")
    void getBalance_api_fail() throws Exception {
        // given
        BalanceInquiryRequest request = BalanceInquiryRequest.builder()
                .accountNo("0000")
                .bankCode("001")
                .customerRrnPrefix("9501011")
                .encryptedKey("key")
                .jwsSignature("sig")
                .build();

        given(accountInquiryService.getBalance(any())).willThrow(new BusinessException(ErrorCode.TRANSFER_DEPOSIT_ACCOUNT_FAULT));

        // when & then
        mockMvc.perform(post("/api/v1/bank/inquiry/balance")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("TRANSFER_002"))
                .andDo(print());
    }
}

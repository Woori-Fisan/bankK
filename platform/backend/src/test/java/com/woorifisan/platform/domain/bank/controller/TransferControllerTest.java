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
import com.woorifisan.platform.domain.bank.dto.response.TransferRecipientResponse;
import com.woorifisan.platform.domain.bank.service.TransferService;
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

@WebMvcTest(TransferController.class)
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
class TransferControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
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
}

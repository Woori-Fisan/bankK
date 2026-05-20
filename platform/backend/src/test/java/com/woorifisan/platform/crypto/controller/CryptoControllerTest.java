package com.woorifisan.platform.crypto.controller;

import com.woorifisan.platform.crypto.service.CryptoService;
import com.woorifisan.platform.crypto.dto.PublicKeyResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class CryptoControllerTest {

    private MockMvc mockMvc;

    private CryptoService cryptoService;

    @BeforeEach
    void setUp() {
        cryptoService = Mockito.mock(CryptoService.class);

        CryptoController controller =
                new CryptoController(cryptoService);

        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .build();
    }

    @Test
    void RSA_공개키_조회_요청_시_정상_응답과_공개키를_반환한다() throws Exception {
        // given
        String mockPublicKey = "MOCK_PUBLIC_KEY_FROM_SERVICE";
        String bankCode = "WOORI";

        when(cryptoService.getPublicKey(bankCode)).thenReturn(new PublicKeyResponse(bankCode, mockPublicKey));

        // when & then
        mockMvc.perform(get("/api/v1/crypto/public-key").param("bankCode", bankCode))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.publicKey").value(mockPublicKey));
    }
}
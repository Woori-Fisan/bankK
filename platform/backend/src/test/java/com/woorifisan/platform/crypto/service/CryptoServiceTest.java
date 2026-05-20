package com.woorifisan.platform.crypto.service;

import com.woorifisan.platform.global.exception.BusinessException;
import com.woorifisan.platform.crypto.dto.PublicKeyResponse;
import com.woorifisan.platform.crypto.dto.BankApiResponse;
import com.woorifisan.platform.crypto.mapper.BankRsaKeyMapper;
import com.woorifisan.platform.crypto.infra.BankCryptoClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class CryptoServiceTest {

    @Mock
    private BankRsaKeyMapper bankRsaKeyMapper;

    @Mock
    private BankCryptoClient bankCryptoClient;

    @InjectMocks
    private CryptoService cryptoService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void RSA_공개키를_조회하면_Public_Key_문자열을_반환한다() {
        // given
        String mockPublicKey = "MOCK_PUBLIC_KEY_BASE64";
        String bankCode = "020";
        String keyId = "key-123";

        BankApiResponse mockResponse = new BankApiResponse(bankCode, keyId, mockPublicKey);

        when(bankRsaKeyMapper.existsByBankCode(bankCode)).thenReturn(true);
        when(bankCryptoClient.fetchPublicKeyFromBank(bankCode)).thenReturn(mockResponse);
        doNothing().when(bankRsaKeyMapper).updateBankRsaKey(bankCode, keyId, mockPublicKey);

        // when
        PublicKeyResponse result = cryptoService.getPublicKey(bankCode);

        // then
        assertNotNull(result);
        assertEquals(mockPublicKey, result.getPublicKey());
        verify(bankRsaKeyMapper).updateBankRsaKey(bankCode, keyId, mockPublicKey);
    }

    @Test
    void 은행_API_응답이_NULL이면_BANK_API_ERROR_예외가_발생한다() {
        // given
        String bankCode = "020";
        when(bankRsaKeyMapper.existsByBankCode(bankCode)).thenReturn(true);
        when(bankCryptoClient.fetchPublicKeyFromBank(bankCode)).thenReturn(null);

        // when & then
        BusinessException exception = assertThrows(BusinessException.class, () -> cryptoService.getPublicKey(bankCode));
        assertEquals(com.woorifisan.platform.global.response.ErrorCode.BANK_API_ERROR, exception.getErrorCode());
    }
}

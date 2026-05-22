package com.woorifisan.platform.domain.bank.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.woorifisan.platform.domain.bank.dto.request.WithdrawalRequest;
import com.woorifisan.platform.domain.bank.dto.response.TransferResponse;
import com.woorifisan.platform.domain.bank.external.client.BankExternalClient;
import com.woorifisan.platform.domain.bank.external.dto.BankWithdrawalRequest;
import com.woorifisan.platform.global.exception.BusinessException;
import com.woorifisan.platform.global.response.ErrorCode;
import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class WithdrawalServiceTest {

    @Mock
    private BankExternalClient bankExternalClient;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private WithdrawalService withdrawalService;

    @Test
    @DisplayName("출금 실행 성공 케이스")
    void executeWithdraw_Success() {
        // given
        WithdrawalRequest request = new WithdrawalRequest();
        request.setWithdrawalBankCode("020");
        request.setWithdrawalAccountNo("123-456");
        request.setWithdrawalPassword("1234");
        request.setAmount(new BigDecimal("10000"));
        request.setEncryptedKey("encKey");
        request.setJwsSignature("signature");
        request.setCustomerRrnPrefix("9001014");

        String encodedPassword = "encoded_1234";
        when(passwordEncoder.encode("1234")).thenReturn(encodedPassword);

        TransferResponse expectedResponse = TransferResponse.builder()
                .transactionId("TRX-001")
                .transactionDate("2024-05-22 10:00:00")
                .balanceAfter(new BigDecimal("90000"))
                .build();

        when(bankExternalClient.withdraw(eq("020"), any(BankWithdrawalRequest.class)))
                .thenReturn(expectedResponse);

        // when
        TransferResponse actualResponse = withdrawalService.executeWithdraw(request);

        // then
        assertThat(actualResponse).isNotNull();
        assertThat(actualResponse.getTransactionId()).isEqualTo("TRX-001");
        
        verify(passwordEncoder).encode("1234");
        verify(bankExternalClient).withdraw(eq("020"), any(BankWithdrawalRequest.class));
    }

    @Test
    @DisplayName("은행 API 호출 중 오류 발생 시 BusinessException 발생")
    void executeWithdraw_Fail_BankApiError() {
        // given
        WithdrawalRequest request = new WithdrawalRequest();
        request.setWithdrawalBankCode("020");
        request.setWithdrawalAccountNo("123-456");
        request.setWithdrawalPassword("1234");
        request.setAmount(new BigDecimal("10000"));

        lenient().when(passwordEncoder.encode(any())).thenReturn("encoded");
        when(bankExternalClient.withdraw(any(), any()))
                .thenThrow(new BusinessException(ErrorCode.BANK_API_ERROR));

        // when & then
        assertThatThrownBy(() -> withdrawalService.executeWithdraw(request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.BANK_API_ERROR);
    }

    @Test
    @DisplayName("잔액 부족 에러 발생 시 BusinessException 발생")
    void executeWithdraw_Fail_InsufficientBalance() {
        // given
        WithdrawalRequest request = new WithdrawalRequest();
        request.setWithdrawalBankCode("020");
        request.setWithdrawalAccountNo("123-456");
        request.setWithdrawalPassword("1234");
        request.setAmount(new BigDecimal("1000000"));

        lenient().when(passwordEncoder.encode(any())).thenReturn("encoded");
        when(bankExternalClient.withdraw(any(), any()))
                .thenThrow(new BusinessException(ErrorCode.TRANSFER_WITHDRAW_AMOUNT_FAULT));

        // when & then
        assertThatThrownBy(() -> withdrawalService.executeWithdraw(request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.TRANSFER_WITHDRAW_AMOUNT_FAULT);
    }
}

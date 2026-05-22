package com.woorifisan.platform.domain.bank.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
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
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WithdrawalServiceTest {

    @Mock
    private BankExternalClient bankExternalClient;

    @InjectMocks
    private WithdrawalService withdrawalService;

    @Test
    @DisplayName("출금 실행 성공 케이스 - 비밀번호 평문 및 JWS 서명 전달 확인")
    void executeWithdraw_Success() {
        // given
        WithdrawalRequest request = new WithdrawalRequest();
        request.setWithdrawalBankCode("020");
        request.setWithdrawalAccountNo("123-456");
        request.setWithdrawalPassword("1234");
        request.setAmount(new BigDecimal("10000"));
        request.setEncryptedKey("encKey");
        request.setCustomerRrnPrefix("900101");

        String jwsSignature = "signature_from_header";

        TransferResponse expectedResponse = TransferResponse.builder()
                .transactionId("TRX-001")
                .transactionDate("2024-05-22 10:00:00")
                .balanceAfter(new BigDecimal("90000"))
                .build();

        when(bankExternalClient.withdraw(eq("020"), any(BankWithdrawalRequest.class)))
                .thenReturn(expectedResponse);

        // when
        TransferResponse actualResponse = withdrawalService.executeWithdraw(request, jwsSignature);

        // then
        assertThat(actualResponse).isNotNull();
        assertThat(actualResponse.getTransactionId()).isEqualTo("TRX-001");
        
        // BankExternalClient로 전달된 요청 캡처하여 데이터 확인
        ArgumentCaptor<BankWithdrawalRequest> captor = ArgumentCaptor.forClass(BankWithdrawalRequest.class);
        verify(bankExternalClient).withdraw(eq("020"), captor.capture());
        
        assertThat(captor.getValue().getWithdrawalPassword()).isEqualTo("1234"); // 평문 확인
        assertThat(captor.getValue().getJwsSignature()).isEqualTo(jwsSignature); // 헤더에서 온 서명 확인
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

        when(bankExternalClient.withdraw(anyString(), any()))
                .thenThrow(new BusinessException(ErrorCode.BANK_API_ERROR));

        // when & then
        assertThatThrownBy(() -> withdrawalService.executeWithdraw(request, "sig"))
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

        when(bankExternalClient.withdraw(anyString(), any()))
                .thenThrow(new BusinessException(ErrorCode.TRANSFER_WITHDRAW_AMOUNT_FAULT));

        // when & then
        assertThatThrownBy(() -> withdrawalService.executeWithdraw(request, "sig"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.TRANSFER_WITHDRAW_AMOUNT_FAULT);
    }
}

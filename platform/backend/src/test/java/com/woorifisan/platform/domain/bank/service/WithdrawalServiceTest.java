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
    @DisplayName("출금 실행 성공 케이스 - reqPayload와 금액이 은행 클라이언트로 올바르게 전달됨")
    void executeWithdraw_Success() {
        // given
        WithdrawalRequest request = WithdrawalRequest.builder()
                .reqPayload("encryptedPayload")
                .withdrawalBankCode("020")
                .amount(new BigDecimal("10000"))
                .build();

        TransferResponse expectedResponse = TransferResponse.builder()
                .transactionId("TRX-001")
                .transactionDate("2024-05-22 10:00:00")
                .balanceAfter(new BigDecimal("90000"))
                .build();

        when(bankExternalClient.withdraw(eq("020"), any(BankWithdrawalRequest.class)))
                .thenReturn(expectedResponse);

        // when
        TransferResponse actualResponse = withdrawalService.executeWithdraw(request, "test-key-id");

        // then
        assertThat(actualResponse).isNotNull();
        assertThat(actualResponse.getTransactionId()).isEqualTo("TRX-001");

        // BankExternalClient로 전달된 요청 캡처하여 E2EE pass-through 확인
        ArgumentCaptor<BankWithdrawalRequest> captor = ArgumentCaptor.forClass(BankWithdrawalRequest.class);
        verify(bankExternalClient).withdraw(eq("020"), captor.capture());

        assertThat(captor.getValue().getReqPayload()).isEqualTo("encryptedPayload");
        assertThat(captor.getValue().getAmount()).isEqualByComparingTo(new BigDecimal("10000"));
    }

    @Test
    @DisplayName("은행 API 호출 중 오류 발생 시 BusinessException 발생")
    void executeWithdraw_Fail_BankApiError() {
        // given
        WithdrawalRequest request = WithdrawalRequest.builder()
                .reqPayload("encryptedPayload")
                .withdrawalBankCode("020")
                .amount(new BigDecimal("10000"))
                .build();

        when(bankExternalClient.withdraw(anyString(), any()))
                .thenThrow(new BusinessException(ErrorCode.BANK_API_ERROR));

        // when & then
        assertThatThrownBy(() -> withdrawalService.executeWithdraw(request, "test-key-id"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.BANK_API_ERROR);
    }

    @Test
    @DisplayName("잔액 부족 에러 발생 시 BusinessException 발생")
    void executeWithdraw_Fail_InsufficientBalance() {
        // given
        WithdrawalRequest request = WithdrawalRequest.builder()
                .reqPayload("encryptedPayload")
                .withdrawalBankCode("020")
                .amount(new BigDecimal("1000000"))
                .build();

        when(bankExternalClient.withdraw(anyString(), any()))
                .thenThrow(new BusinessException(ErrorCode.TRANSFER_WITHDRAW_AMOUNT_FAULT));

        // when & then
        assertThatThrownBy(() -> withdrawalService.executeWithdraw(request, "test-key-id"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.TRANSFER_WITHDRAW_AMOUNT_FAULT);
    }
}

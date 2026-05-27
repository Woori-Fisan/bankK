package com.woorifisan.platform.domain.bank.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

import com.woorifisan.platform.domain.bank.dto.request.TransferRecipientRequest;
import com.woorifisan.platform.domain.bank.dto.request.TransferRequest;
import com.woorifisan.platform.domain.bank.dto.response.TransferRecipientResponse;
import com.woorifisan.platform.domain.bank.dto.response.TransferResponse;
import com.woorifisan.platform.domain.bank.external.client.BankExternalClient;
import com.woorifisan.platform.domain.bank.external.dto.BankRecipientResponse;
import com.woorifisan.platform.domain.bank.external.dto.BankTransferResponse;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TransferServiceTest {

    @InjectMocks
    private TransferService transferService;

    @Mock
    private BankExternalClient bankExternalClient;

    @Test
    @DisplayName("수취인 조회 성공 테스트")
    void getRecipient_success() {
        // given
        TransferRecipientRequest request = new TransferRecipientRequest();
        request.setDepositBankCode("020");
        request.setDepositAccountNo("12345678");

        BankRecipientResponse mockBankResponse = BankRecipientResponse.builder()
                .depositorName("홍길동")
                .depositBankName("우리은행")
                .depositAccountNo("12345678")
                .accountStatus("NORMAL")
                .build();

        given(bankExternalClient.fetchRecipient(eq("020"), any())).willReturn(mockBankResponse);

        // when
        TransferRecipientResponse response = transferService.getRecipient(request);

        // then
        assertThat(response.getDepositorName()).isEqualTo("홍길동");
        assertThat(response.getDepositBankAccountNo()).isEqualTo("12345678");
    }

    @Test
    @DisplayName("당행 이체 실행 성공 테스트")
    void executeTransfer_internal_success() {
        // given
        TransferRequest request = new TransferRequest();
        request.setWithdrawalBankCode("020");
        request.setWithdrawalAccountNo("11112222");
        request.setWithdrawalPassword("1234");
        request.setCustomerRrnPrefix("9001011");
        request.setDepositBankCode("020");
        request.setDepositAccountNo("33334444");
        request.setAmount(new BigDecimal("10000"));

        BankTransferResponse mockBankResponse = BankTransferResponse.builder()
                .transactionId(UUID.randomUUID().toString())
                .transactionDate("2026-05-27 10:00:00")
                .balanceAfter(new BigDecimal("500000"))
                .build();

        given(bankExternalClient.executeTransfer(eq("020"), any())).willReturn(mockBankResponse);

        // when
        TransferResponse response = transferService.executeTransfer(request);

        // then
        assertThat(response.getTransactionId()).isEqualTo(mockBankResponse.getTransactionId());
        assertThat(response.getTransactionDate()).isEqualTo("2026-05-27");
        assertThat(response.getBalanceAfter()).isEqualByComparingTo(new BigDecimal("500000"));
    }

    @Test
    @DisplayName("타행 이체 실행 성공 테스트")
    void executeTransfer_external_success() {
        // given
        TransferRequest request = new TransferRequest();
        request.setWithdrawalBankCode("020");
        request.setWithdrawalAccountNo("11112222");
        request.setWithdrawalPassword("1234");
        request.setCustomerRrnPrefix("9001011");
        request.setDepositBankCode("004");
        request.setDepositAccountNo("33334444");
        request.setAmount(new BigDecimal("10000"));

        BankTransferResponse mockWithdrawResponse = BankTransferResponse.builder()
                .transactionId(UUID.randomUUID().toString())
                .balanceAfter(new BigDecimal("490000"))
                .build();

        BankTransferResponse mockDepositResponse = BankTransferResponse.builder()
                .transactionId(UUID.randomUUID().toString())
                .build();

        given(bankExternalClient.fetchTransferWithdraw(eq("020"), any())).willReturn(mockWithdrawResponse);
        given(bankExternalClient.fetchDeposit(eq("004"), any())).willReturn(mockDepositResponse);

        // when
        TransferResponse response = transferService.executeTransfer(request);

        // then
        assertThat(response.getTransactionId()).isEqualTo(mockDepositResponse.getTransactionId());
        assertThat(response.getBalanceAfter()).isEqualByComparingTo(new BigDecimal("490000"));
    }
}

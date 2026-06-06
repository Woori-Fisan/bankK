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
        request.setReqPayload("encrypted-payload");

        TransferRecipientResponse mockBankResponse = TransferRecipientResponse.builder()
                .resPayload("encrypted-res-payload")
                .depositBankName("우리은행")
                .accountStatus("NORMAL")
                .build();

        given(bankExternalClient.fetchRecipient(eq("020"), any())).willReturn(mockBankResponse);

        // when
        TransferRecipientResponse response = transferService.getRecipient(request, "bank-key-id");

        // then
        assertThat(response.getDepositBankName()).isEqualTo("우리은행");
        assertThat(response.getAccountStatus()).isEqualTo("NORMAL");
        assertThat(response.getResPayload()).isEqualTo("encrypted-res-payload");
    }

    @Test
    @DisplayName("이체 실행 성공 테스트 (통합 로직)")
    void executeTransfer_success() {
        // given
        TransferRequest request = new TransferRequest();
        request.setWithdrawalBankCode("020");
        request.setDepositBankCode("004");
        request.setAmount(new BigDecimal("10000"));
        request.setWithdrawReqPayload("withdraw-jwe");
        request.setDepositReqPayload("deposit-jwe");

        BankTransferResponse mockTransferResponse = BankTransferResponse.builder()
                .transactionId(UUID.randomUUID().toString())
                .transactionDate("2026-05-27 10:00:00")
                .balanceAfter(new BigDecimal("500000"))
                .resPayload("withdraw-res-jwe")
                .build();

        given(bankExternalClient.fetchTransferExecute(eq("020"), any())).willReturn(mockTransferResponse);

        // when
        TransferResponse response = transferService.executeTransfer(request, "jws-sig", "w-key", "d-key");

        // then
        assertThat(response.getTransactionId()).isEqualTo(mockTransferResponse.getTransactionId());
        assertThat(response.getTransactionDate()).isEqualTo("2026-05-27");
        assertThat(response.getBalanceAfter()).isEqualByComparingTo(new BigDecimal("500000"));
        assertThat(response.getResPayload()).isEqualTo("withdraw-res-jwe");
    }
}

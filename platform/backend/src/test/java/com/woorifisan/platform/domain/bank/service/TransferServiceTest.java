package com.woorifisan.platform.domain.bank.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

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
    @DisplayName("수취인 조회 시 외부 연동 API 에러 발생 시 예외가 전파된다")
    void getRecipient_externalClientException() {
        // given
        TransferRecipientRequest request = new TransferRecipientRequest();
        request.setDepositBankCode("020");

        given(bankExternalClient.fetchRecipient(eq("020"), any()))
                .willThrow(new RuntimeException("Connection timeout"));

        // when & then
        assertThatThrownBy(() -> transferService.getRecipient(request, "bank-key-id"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Connection timeout");
    }

    @Test
    @DisplayName("이체 실행 성공 테스트 (통합 로직)")
    void executeTransfer_success() {
        // given
        TransferRequest request = new TransferRequest();
        request.setWithdrawalBankCode("020");
        request.setDepositBankCode("004");
        request.setAmount(new BigDecimal("10000"));
        request.setReqPayload("withdraw-jwe");

        BankTransferResponse mockTransferResponse = BankTransferResponse.builder()
                .transactionId(UUID.randomUUID().toString())
                .transactionDate("2026-05-27 10:00:00")
                .balanceAfter(new BigDecimal("500000"))
                .resPayload("withdraw-res-jwe")
                .build();

        given(bankExternalClient.fetchTransferExecute(eq("020"), any())).willReturn(mockTransferResponse);

        // when
        TransferResponse response = transferService.executeTransfer(request, "jws-sig", "w-key");

        // then
        assertThat(response.getTransactionId()).isEqualTo(mockTransferResponse.getTransactionId());
        assertThat(response.getTransactionDate()).isEqualTo("2026-05-27 10:00:00");
        assertThat(response.getBalanceAfter()).isEqualByComparingTo(new BigDecimal("500000"));
        assertThat(response.getResPayload()).isEqualTo("withdraw-res-jwe");
    }

    @Test
    @DisplayName("이체 실행 시 거래 일시가 null이면 현재 시각이 기본 포맷으로 적용된다")
    void executeTransfer_nullTransactionDate() {
        // given
        TransferRequest request = new TransferRequest();
        request.setWithdrawalBankCode("020");
        request.setDepositBankCode("004");
        request.setAmount(new BigDecimal("10000"));

        BankTransferResponse mockTransferResponse = BankTransferResponse.builder()
                .transactionId("tx-123")
                .transactionDate(null) // null 날짜
                .balanceAfter(new BigDecimal("90000"))
                .resPayload("res-payload")
                .build();

        given(bankExternalClient.fetchTransferExecute(eq("020"), any())).willReturn(mockTransferResponse);

        String beforeExecution = LocalDateTime.now().format(DATE_FORMATTER);

        // when
        TransferResponse response = transferService.executeTransfer(request, "jws-sig", "w-key");

        // then
        String afterExecution = LocalDateTime.now().format(DATE_FORMATTER);
        assertThat(response.getTransactionDate()).isNotNull();
        // 실행 전후 시각 사이에 정상적으로 시간이 포맷팅되었는지 확인
        assertThat(response.getTransactionDate()).isGreaterThanOrEqualTo(beforeExecution);
        assertThat(response.getTransactionDate()).isLessThanOrEqualTo(afterExecution);
    }

    @Test
    @DisplayName("이체 실행 시 거래 일시가 잘못된 포맷이면 예외를 캐치하고 현재 시각으로 대체된다")
    void executeTransfer_invalidTransactionDateFormat() {
        // given
        TransferRequest request = new TransferRequest();
        request.setWithdrawalBankCode("020");
        request.setDepositBankCode("004");
        request.setAmount(new BigDecimal("10000"));

        BankTransferResponse mockTransferResponse = BankTransferResponse.builder()
                .transactionId("tx-123")
                .transactionDate("invalid-date-format") // 유효하지 않은 날짜 포맷
                .balanceAfter(new BigDecimal("90000"))
                .resPayload("res-payload")
                .build();

        given(bankExternalClient.fetchTransferExecute(eq("020"), any())).willReturn(mockTransferResponse);

        String beforeExecution = LocalDateTime.now().format(DATE_FORMATTER);

        // when
        TransferResponse response = transferService.executeTransfer(request, "jws-sig", "w-key");

        // then
        String afterExecution = LocalDateTime.now().format(DATE_FORMATTER);
        assertThat(response.getTransactionDate()).isNotNull();
        assertThat(response.getTransactionDate()).isGreaterThanOrEqualTo(beforeExecution);
        assertThat(response.getTransactionDate()).isLessThanOrEqualTo(afterExecution);
    }

    @Test
    @DisplayName("이체 실행 시 외부 연동 API 에러 발생 시 예외가 전파된다")
    void executeTransfer_externalClientException() {
        // given
        TransferRequest request = new TransferRequest();
        request.setWithdrawalBankCode("020");
        request.setAmount(new BigDecimal("10000"));

        given(bankExternalClient.fetchTransferExecute(eq("020"), any()))
                .willThrow(new RuntimeException("Connection timeout"));

        // when & then
        assertThatThrownBy(() -> transferService.executeTransfer(request, "jws-sig", "w-key"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Connection timeout");
    }
}

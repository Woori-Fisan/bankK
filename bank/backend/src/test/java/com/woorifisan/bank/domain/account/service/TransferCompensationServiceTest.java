package com.woorifisan.bank.domain.account.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.woorifisan.bank.domain.account.dto.decrypted.DecryptedWithdrawData;
import com.woorifisan.bank.domain.account.dto.request.InternalDepositRequest;
import com.woorifisan.bank.domain.account.dto.request.TransferRequest;
import com.woorifisan.bank.domain.account.dto.response.TransferStatusResponse;
import com.woorifisan.bank.global.config.BankNetworkConfig;
import com.woorifisan.bank.global.response.ApiResponse;
import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.ResponseEntity;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * TransferCompensationService 단위 테스트
 *
 * Spring 컨텍스트 없이 실행되므로 {@code @Async} 프록시가 생성되지 않아
 * compensate()는 동기적으로 실행된다. WebClient 체인은 Mockito로 모킹한다.
 */
@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"unchecked", "rawtypes"})
class TransferCompensationServiceTest {

    @InjectMocks
    private TransferCompensationService compensationService;

    @Mock
    private WebClient webClient;

    @Mock
    private BankNetworkConfig bankNetworkConfig;

    @Mock
    private TransferTxService transferTxService;

    // POST 체인 목 객체
    @Mock
    private WebClient.RequestBodyUriSpec postUriSpec;
    @Mock
    private WebClient.RequestBodySpec postBodySpec;
    @Mock
    private WebClient.ResponseSpec postResponseSpec;

    // GET 체인 목 객체
    @Mock
    private WebClient.RequestHeadersUriSpec getUriSpec;
    @Mock
    private WebClient.RequestHeadersSpec getHeadersSpec;
    @Mock
    private WebClient.ResponseSpec getResponseSpec;

    private static final String DEPOSIT_BANK_CODE = "002";
    private static final String TX_ID = "tx-test-001";

    private InternalDepositRequest depositRequest;
    private TransferRequest originalRequest;
    private DecryptedWithdrawData decryptedData;
    private BankNetworkConfig.BankProperty bankProperty;

    @BeforeEach
    void setUp() {
        depositRequest = InternalDepositRequest.builder()
                .depositAccountNo("1234567890")
                .amount(new BigDecimal("50000"))
                .withdrawalBankCode("001")
                .withdrawalAccountNo("0987654321")
                .txId(TX_ID)
                .build();

        originalRequest = TransferRequest.builder()
                .withdrawalBankCode("001")
                .depositBankCode(DEPOSIT_BANK_CODE)
                .amount(new BigDecimal("50000"))
                .reqPayload("dummy-payload")
                .bankKeyId("test-key")
                .build();

        decryptedData = DecryptedWithdrawData.builder()
                .withdrawalAccountNo("0987654321")
                .withdrawalPassword("1234")
                .customerRrnPrefix("900101")
                .customerName("테스트유저")
                .depositAccountNo("1234567890")
                .build();

        bankProperty = new BankNetworkConfig.BankProperty();
        bankProperty.setBaseUrl("http://test-bank");
    }

    // ─────────────────── WebClient 목 헬퍼 ───────────────────

    private void givenDepositSuccess() {
        given(webClient.post()).willReturn(postUriSpec);
        given(postUriSpec.uri(anyString())).willReturn(postBodySpec);
        // bodyValue()의 반환 타입이 RequestHeadersSpec<?>인데 RequestBodySpec이 wildcard에 맞지 않아
        // willAnswer로 타입 검사를 우회한다
        given(postBodySpec.bodyValue(any())).willAnswer(inv -> postBodySpec);
        given(postBodySpec.retrieve()).willReturn(postResponseSpec);
        given(postResponseSpec.onStatus(any(), any())).willReturn(postResponseSpec);
        given(postResponseSpec.toBodilessEntity())
                .willReturn(Mono.just(ResponseEntity.ok().build()));
    }

    private void givenDepositFails(Throwable cause) {
        given(webClient.post()).willReturn(postUriSpec);
        given(postUriSpec.uri(anyString())).willReturn(postBodySpec);
        given(postBodySpec.bodyValue(any())).willAnswer(inv -> postBodySpec);
        given(postBodySpec.retrieve()).willReturn(postResponseSpec);
        given(postResponseSpec.onStatus(any(), any())).willReturn(postResponseSpec);
        given(postResponseSpec.toBodilessEntity()).willReturn(Mono.error(cause));
    }

    private void givenPollReturnsStatus(String status) {
        ApiResponse<TransferStatusResponse> response = ApiResponse.success(
                TransferStatusResponse.builder().txId(TX_ID).status(status).build());
        given(webClient.get()).willReturn(getUriSpec);
        given(getUriSpec.uri(anyString())).willReturn(getHeadersSpec);
        given(getHeadersSpec.retrieve()).willReturn(getResponseSpec);
        given(getResponseSpec.onStatus(any(), any())).willReturn(getResponseSpec);
        given(getResponseSpec.bodyToMono(any(ParameterizedTypeReference.class)))
                .willReturn(Mono.just(response));
    }

    // ─────────────────── 테스트 케이스 ───────────────────

    @Test
    @DisplayName("타행 입금 성공 시 원장을 SUCCESS로 업데이트한다")
    void compensate_depositSuccess_updatesLedgerSuccess() {
        given(bankNetworkConfig.getBankProperty(DEPOSIT_BANK_CODE)).willReturn(bankProperty);
        givenDepositSuccess();

        compensationService.compensate(DEPOSIT_BANK_CODE, depositRequest, originalRequest, decryptedData, TX_ID);

        verify(transferTxService).updateLedgerStatus(TX_ID, "SUCCESS");
        verify(transferTxService, never()).refundTransfer(any(), any(), anyString());
    }

    @Test
    @DisplayName("타행 입금 실패 후 상태 폴링에서 SUCCESS 확인 시 원장을 SUCCESS로 업데이트한다")
    void compensate_depositFails_pollReturnsSuccess_updatesLedgerSuccess() {
        given(bankNetworkConfig.getBankProperty(DEPOSIT_BANK_CODE)).willReturn(bankProperty);
        givenDepositFails(new RuntimeException("네트워크 오류"));
        givenPollReturnsStatus("SUCCESS");

        compensationService.compensate(DEPOSIT_BANK_CODE, depositRequest, originalRequest, decryptedData, TX_ID);

        verify(transferTxService).updateLedgerStatus(TX_ID, "SUCCESS");
        verify(transferTxService, never()).refundTransfer(any(), any(), anyString());
    }

    @Test
    @DisplayName("타행 입금 실패 후 상태 폴링에서 FAILED 확인 시 환불하고 원장을 FAILED로 업데이트한다")
    void compensate_depositFails_pollReturnsFailed_refundsAndUpdatesLedgerFailed() {
        given(bankNetworkConfig.getBankProperty(DEPOSIT_BANK_CODE)).willReturn(bankProperty);
        givenDepositFails(new RuntimeException("네트워크 오류"));
        givenPollReturnsStatus("FAILED");

        compensationService.compensate(DEPOSIT_BANK_CODE, depositRequest, originalRequest, decryptedData, TX_ID);

        verify(transferTxService).refundTransfer(originalRequest, decryptedData, TX_ID);
        verify(transferTxService).updateLedgerStatus(TX_ID, "FAILED");
    }

    @Test
    @DisplayName("타행 입금 실패 후 상태 폴링에서 PENDING 응답 → 재시도 → SUCCESS 확인 시 원장을 SUCCESS로 업데이트한다")
    void compensate_depositFails_pollPendingThenSuccess_updatesLedgerSuccess() {
        // retryWhen은 bodyToMono가 반환한 동일한 Mono를 재구독하므로,
        // Mono.defer()로 감싸야 구독마다 다른 값을 내보낼 수 있다 (약 1초 소요)
        given(bankNetworkConfig.getBankProperty(DEPOSIT_BANK_CODE)).willReturn(bankProperty);
        givenDepositFails(new RuntimeException("입금 요청 실패"));

        ApiResponse<TransferStatusResponse> pending = ApiResponse.success(
                TransferStatusResponse.builder().txId(TX_ID).status("PENDING").build());
        ApiResponse<TransferStatusResponse> success = ApiResponse.success(
                TransferStatusResponse.builder().txId(TX_ID).status("SUCCESS").build());

        AtomicInteger subscriptionCount = new AtomicInteger(0);
        given(webClient.get()).willReturn(getUriSpec);
        given(getUriSpec.uri(anyString())).willReturn(getHeadersSpec);
        given(getHeadersSpec.retrieve()).willReturn(getResponseSpec);
        given(getResponseSpec.onStatus(any(), any())).willReturn(getResponseSpec);
        given(getResponseSpec.bodyToMono(any(ParameterizedTypeReference.class)))
                .willReturn(Mono.defer(() ->
                        subscriptionCount.incrementAndGet() == 1
                                ? Mono.just(pending)
                                : Mono.just(success)));

        compensationService.compensate(DEPOSIT_BANK_CODE, depositRequest, originalRequest, decryptedData, TX_ID);

        verify(transferTxService).updateLedgerStatus(TX_ID, "SUCCESS");
        verify(transferTxService, never()).refundTransfer(any(), any(), anyString());
    }
}

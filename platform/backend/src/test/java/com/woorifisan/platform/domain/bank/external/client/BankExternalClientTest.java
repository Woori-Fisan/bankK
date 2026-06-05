package com.woorifisan.platform.domain.bank.external.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.woorifisan.platform.domain.bank.dto.response.BalanceInquiryResponse;
import com.woorifisan.platform.domain.bank.dto.response.HistoryInquiryResponse;
import com.woorifisan.platform.domain.bank.dto.response.TransferResponse;
import com.woorifisan.platform.domain.bank.external.dto.BankBalanceInquiryRequest;
import com.woorifisan.platform.domain.bank.external.dto.BankHistoryInquiryRequest;
import com.woorifisan.platform.domain.bank.external.dto.BankWithdrawalRequest;
import com.woorifisan.platform.global.config.BankNetworkConfig;
import com.woorifisan.platform.global.config.BankNetworkConfig.BankProperty;
import com.woorifisan.platform.global.exception.BusinessException;
import com.woorifisan.platform.global.response.ApiResponse;
import com.woorifisan.platform.global.response.ErrorCode;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class BankExternalClientTest {

    @Mock
    private WebClient webClient;

    @Mock
    private BankNetworkConfig bankNetworkConfig;

    @Mock
    private WebClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    private WebClient.RequestBodySpec requestBodySpec;

    @Mock
    private WebClient.RequestHeadersSpec requestHeadersSpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

    @InjectMocks
    private BankExternalClient bankExternalClient;

    private final String bankCode = "020";

    @BeforeEach
    void setUp() {
        // 공통적인 WebClient 체이닝 모킹 설정
        // 일부 테스트(실패 케이스)에서 사용되지 않을 수 있으므로 lenient()를 사용합니다.
        lenient().when(webClient.post()).thenReturn(requestBodyUriSpec);
        lenient().when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        lenient().when(requestBodySpec.contentType(MediaType.APPLICATION_JSON)).thenReturn(requestBodySpec);
        lenient().when(requestBodySpec.bodyValue(any())).thenReturn(requestHeadersSpec);
        lenient().when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
    }

    @Test
    @DisplayName("잔액 조회 성공 케이스")
    void fetchBalance_Success() {
        // given
        BankProperty bankProperty = new BankProperty();
        bankProperty.setBaseUrl("http://bank-core");
        bankProperty.getEndpoints().put("balance", "/api/v1/accounts/balance");

        when(bankNetworkConfig.getBankProperty(bankCode)).thenReturn(bankProperty);

        BankBalanceInquiryRequest request = BankBalanceInquiryRequest.of("encryptedPayload", "test-key-id");
        BalanceInquiryResponse expectedData = BalanceInquiryResponse.builder()
                .status("NORMAL")
                .build();
        ApiResponse<BalanceInquiryResponse> apiResponse = ApiResponse.success(expectedData);

        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(any(ParameterizedTypeReference.class))).thenReturn(Mono.just(apiResponse));

        // when
        BalanceInquiryResponse actualResponse = bankExternalClient.fetchBalance(bankCode, request);

        // then
        assertThat(actualResponse).isNotNull();
        assertThat(actualResponse.getStatus()).isEqualTo("NORMAL");
    }

    @Test
    @DisplayName("지원하지 않는 은행 코드인 경우 실패")
    void fetchBalance_Fail_BankNotFound() {
        // given
        when(bankNetworkConfig.getBankProperty("999")).thenReturn(null);
        BankBalanceInquiryRequest request = BankBalanceInquiryRequest.of("encryptedPayload", "test-key-id");

        // when & then
        assertThatThrownBy(() -> bankExternalClient.fetchBalance("999", request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.BANK_NOT_FOUND);
    }

    @Test
    @DisplayName("외부 은행 API에서 에러 응답(4xx/5xx)을 받은 경우 실패")
    void fetchBalance_Fail_ApiError() {
        // given
        BankProperty bankProperty = new BankProperty();
        bankProperty.setBaseUrl("http://bank-core");
        bankProperty.getEndpoints().put("balance", "/api/v1/accounts/balance");

        when(bankNetworkConfig.getBankProperty(bankCode)).thenReturn(bankProperty);

        BankBalanceInquiryRequest request = BankBalanceInquiryRequest.of("encryptedPayload", "test-key-id");

        // WebClient 통신 중 예외 발생 시나리오
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(any(ParameterizedTypeReference.class))).thenReturn(Mono.error(new RuntimeException("Connection Timeout")));

        // when & then
        assertThatThrownBy(() -> bankExternalClient.fetchBalance(bankCode, request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.BANK_API_ERROR);
    }

    @Test
    @DisplayName("잔액 조회 시 응답 바디가 null인 경우 실패")
    void fetchBalance_Fail_NullResponse() {
        // given
        BankProperty bankProperty = new BankProperty();
        bankProperty.setBaseUrl("http://bank-core");
        bankProperty.getEndpoints().put("balance", "/api/v1/accounts/balance");

        when(bankNetworkConfig.getBankProperty(bankCode)).thenReturn(bankProperty);

        BankBalanceInquiryRequest request = BankBalanceInquiryRequest.of("encryptedPayload", "test-key-id");

        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(any(ParameterizedTypeReference.class))).thenReturn(Mono.empty());

        // when & then
        assertThatThrownBy(() -> bankExternalClient.fetchBalance(bankCode, request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.BANK_API_ERROR);
    }

    @Test
    @DisplayName("출금 성공 케이스")
    void withdraw_Success() {
        // given
        BankProperty bankProperty = new BankProperty();
        bankProperty.setBaseUrl("http://bank-core");
        bankProperty.getEndpoints().put("withdraw", "/api/v1/baas/withdrawals");

        when(bankNetworkConfig.getBankProperty(bankCode)).thenReturn(bankProperty);

        BankWithdrawalRequest request = BankWithdrawalRequest.of("encryptedPayload", "test-key-id", new BigDecimal("10000"));
        TransferResponse expectedData = TransferResponse.builder()
                .transactionId("tx-123")
                .balanceAfter(new BigDecimal("90000"))
                .build();
        ApiResponse<TransferResponse> apiResponse = ApiResponse.success(expectedData);

        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(any(ParameterizedTypeReference.class))).thenReturn(Mono.just(apiResponse));

        // when
        TransferResponse actualResponse = bankExternalClient.withdraw(bankCode, request);

        // then
        assertThat(actualResponse).isNotNull();
        assertThat(actualResponse.getTransactionId()).isEqualTo("tx-123");
    }

    @Test
    @DisplayName("출금 시 응답 데이터(data)가 null인 경우 실패")
    void withdraw_Fail_NullData() {
        // given
        BankProperty bankProperty = new BankProperty();
        bankProperty.setBaseUrl("http://bank-core");
        bankProperty.getEndpoints().put("withdraw", "/api/v1/baas/withdrawals");

        when(bankNetworkConfig.getBankProperty(bankCode)).thenReturn(bankProperty);

        BankWithdrawalRequest request = BankWithdrawalRequest.of("encryptedPayload", "test-key-id", new BigDecimal("10000"));
        ApiResponse<TransferResponse> apiResponse = ApiResponse.success(null);

        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(any(ParameterizedTypeReference.class))).thenReturn(Mono.just(apiResponse));

        // when & then
        assertThatThrownBy(() -> bankExternalClient.withdraw(bankCode, request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.BANK_API_ERROR);
    }

    @Test
    @DisplayName("거래 내역 조회 성공 케이스")
    void fetchHistory_Success() {
        // given
        BankProperty bankProperty = new BankProperty();
        bankProperty.setBaseUrl("http://bank-core");
        bankProperty.getEndpoints().put("transaction", "/api/v1/accounts/history");

        when(bankNetworkConfig.getBankProperty(bankCode)).thenReturn(bankProperty);

        BankHistoryInquiryRequest request = BankHistoryInquiryRequest.builder()
                .reqPayload("encryptedPayload")
                .bankKeyId("test-key-id")
                .startDate("2026-05-01")
                .endDate("2026-05-31")
                .page(0)
                .size(10)
                .build();

        HistoryInquiryResponse expectedData = HistoryInquiryResponse.builder()
                .totalCount(0)
                .build();
        ApiResponse<HistoryInquiryResponse> apiResponse = ApiResponse.success(expectedData);

        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(any(ParameterizedTypeReference.class))).thenReturn(Mono.just(apiResponse));

        // when
        HistoryInquiryResponse actualResponse = bankExternalClient.fetchHistory(bankCode, request);

        // then
        assertThat(actualResponse).isNotNull();
        assertThat(actualResponse.getTotalCount()).isEqualTo(0);
    }
}

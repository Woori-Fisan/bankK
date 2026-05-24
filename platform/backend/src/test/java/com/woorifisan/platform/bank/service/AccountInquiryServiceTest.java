package com.woorifisan.platform.bank.service;

import com.woorifisan.platform.domain.bank.dto.request.HistoryInquiryRequest;
import com.woorifisan.platform.domain.bank.dto.response.HistoryInquiryResponse;
import com.woorifisan.platform.domain.bank.service.AccountInquiryService;
import com.woorifisan.platform.global.exception.BusinessException;
import com.woorifisan.platform.global.response.ApiResponse;
import com.woorifisan.platform.global.response.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class AccountInquiryServiceTest {

    private AccountInquiryService accountInquiryService;

    @Mock
    private WebClient bankWebClient;

    @SuppressWarnings("rawtypes")
    @Mock
    private WebClient.RequestBodyUriSpec requestBodyUriSpec;

    @SuppressWarnings("rawtypes")
    @Mock
    private WebClient.RequestBodySpec requestBodySpec;

    @SuppressWarnings("rawtypes")
    @Mock
    private WebClient.RequestHeadersSpec requestHeadersSpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

    @BeforeEach
    void setUp() {
        accountInquiryService = new AccountInquiryService(bankWebClient);
    }

    @Test
    @DisplayName("조회 시작일이 종료일보다 늦으면 예외가 발생한다")
    void 조회_시작일이_종료일보다_늦으면_예외가_발생한다() {
        // given
        HistoryInquiryRequest request = HistoryInquiryRequest.builder()
                .startDate("2026-05-20")
                .endDate("2026-05-19")
                .build();

        // when & then
        assertThatThrownBy(() -> accountInquiryService.getHistory(request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INQUIRY_INVALID_DATE_RANGE);
    }

    @Test
    @DisplayName("은행 서버 통신 성공 시 거래 내역을 반환한다")
    @SuppressWarnings("unchecked")
    void 성공_거래내역조회() {
        // given
        HistoryInquiryRequest request = HistoryInquiryRequest.builder()
                .startDate("2026-05-19")
                .endDate("2026-05-20")
                .page(0)
                .size(20)
                .build();

        HistoryInquiryResponse mockData = HistoryInquiryResponse.builder()
                .totalCount(1)
                .history(List.of())
                .build();
        ApiResponse<HistoryInquiryResponse> apiResponse = ApiResponse.success(mockData);

        given(bankWebClient.post()).willReturn(requestBodyUriSpec);
        given(requestBodyUriSpec.uri(any(String.class))).willReturn(requestBodySpec);
        given(requestBodySpec.bodyValue(any())).willReturn(requestHeadersSpec);
        given(requestHeadersSpec.retrieve()).willReturn(responseSpec);
        given(responseSpec.onStatus(any(Predicate.class), any(Function.class))).willReturn(responseSpec);
        given(responseSpec.bodyToMono(any(ParameterizedTypeReference.class))).willReturn(Mono.just(apiResponse));

        // when
        HistoryInquiryResponse result = accountInquiryService.getHistory(request);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getTotalCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("은행 서버 응답이 실패(success=false)면 은행의 에러 메시지를 포함한 BANK_API_ERROR 예외가 발생한다")
    @SuppressWarnings("unchecked")
    void 실패_은행서버에러_메시지전달() {
        // given
        HistoryInquiryRequest request = HistoryInquiryRequest.builder()
                .startDate("2026-05-19")
                .endDate("2026-05-20")
                .page(0)
                .size(20)
                .build();

        String bankErrorMessage = "유효하지 않은 계좌입니다.";
        ApiResponse<HistoryInquiryResponse> apiResponse = ApiResponse.error(ErrorCode.INQUIRY_ACCOUNT_NOTFOUND, bankErrorMessage);

        given(bankWebClient.post()).willReturn(requestBodyUriSpec);
        given(requestBodyUriSpec.uri(any(String.class))).willReturn(requestBodySpec);
        given(requestBodySpec.bodyValue(any())).willReturn(requestHeadersSpec);
        given(requestHeadersSpec.retrieve()).willReturn(responseSpec);
        given(responseSpec.onStatus(any(Predicate.class), any(Function.class))).willReturn(responseSpec);
        given(responseSpec.bodyToMono(any(ParameterizedTypeReference.class))).willReturn(Mono.just(apiResponse));

        // when & then
        assertThatThrownBy(() -> accountInquiryService.getHistory(request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.BANK_API_ERROR)
                .hasMessageContaining(bankErrorMessage);
    }
}

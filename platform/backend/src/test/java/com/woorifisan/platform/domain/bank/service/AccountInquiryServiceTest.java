package com.woorifisan.platform.domain.bank.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

import com.woorifisan.platform.domain.bank.dto.request.BalanceInquiryRequest;
import com.woorifisan.platform.domain.bank.dto.request.HistoryInquiryRequest;
import com.woorifisan.platform.domain.bank.dto.response.BalanceInquiryResponse;
import com.woorifisan.platform.domain.bank.dto.response.HistoryInquiryResponse;
import com.woorifisan.platform.domain.bank.external.client.BankExternalClient;
import com.woorifisan.platform.domain.bank.external.dto.BankBalanceInquiryRequest;
import com.woorifisan.platform.domain.bank.external.dto.BankHistoryInquiryRequest;
import com.woorifisan.platform.global.exception.BusinessException;
import com.woorifisan.platform.global.response.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AccountInquiryServiceTest {

    @InjectMocks
    private AccountInquiryService accountInquiryService;

    @Mock
    private BankExternalClient bankExternalClient;

    /**
     * 거래 내역 조회 Test (getHistory)
     */
    @Test
    @DisplayName("조회 시작일이 종료일보다 늦으면 예외가 발생한다")
    void 조회_시작일이_종료일보다_늦으면_예외가_발생한다() {
        // given
        HistoryInquiryRequest request = HistoryInquiryRequest.builder()
                .startDate("2026-05-20")
                .endDate("2026-05-19")
                .build();

        // when & then
        assertThatThrownBy(() -> accountInquiryService.getHistory(request, "test-key-id"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INQUIRY_INVALID_DATE_RANGE);
    }

    @Test
    @DisplayName("날짜 형식이 올바르지 않으면 예외가 발생한다")
    void 날짜_형식이_올바르지_않으면_예외가_발생한다() {
        // given
        HistoryInquiryRequest request = HistoryInquiryRequest.builder()
                .startDate("20260520")
                .endDate("20260521")
                .build();

        // when & then
        assertThatThrownBy(() -> accountInquiryService.getHistory(request, "test-key-id"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT);
    }

    @Test
    @DisplayName("은행 서버 통신 성공 시 거래 내역을 반환한다")
    void 성공_거래내역조회() {
        // given
        String bankCode = "020";
        HistoryInquiryRequest request = HistoryInquiryRequest.builder()
                .bankCode(bankCode)
                .startDate("2026-05-19")
                .endDate("2026-05-20")
                .page(0)
                .size(20)
                .build();

        HistoryInquiryResponse mockResponse = HistoryInquiryResponse.builder()
                .totalCount(1)
                .build();

        given(bankExternalClient.fetchHistory(eq(bankCode), any(BankHistoryInquiryRequest.class)))
                .willReturn(mockResponse);

        // when
        HistoryInquiryResponse result = accountInquiryService.getHistory(request, "test-key-id");

        // then
        assertThat(result).isNotNull();
        assertThat(result.getTotalCount()).isEqualTo(1);
    }

    /**
     * 잔액 조회 Test (getBalance)
     */
    @Test
    @DisplayName("잔액 조회 성공")
    void getBalance_Success() {
        // given
        String bankCode = "020";
        BalanceInquiryRequest request = BalanceInquiryRequest.builder()
                .bankCode(bankCode)
                .build();

        BalanceInquiryResponse mockResponse = BalanceInquiryResponse.builder()
                .status("NORMAL")
                .build();

        given(bankExternalClient.fetchBalance(eq(bankCode), any(BankBalanceInquiryRequest.class)))
                .willReturn(mockResponse);

        // when
        BalanceInquiryResponse response = accountInquiryService.getBalance(request, "test-key-id");

        // then
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo("NORMAL");
    }
}

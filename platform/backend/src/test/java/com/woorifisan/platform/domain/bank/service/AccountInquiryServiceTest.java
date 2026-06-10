package com.woorifisan.platform.domain.bank.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

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
import org.mockito.ArgumentCaptor;
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
     * 잔액 조회 테스트 (getBalance)
     */
    @Test
    @DisplayName("잔액 조회 성공 시 암호화된 페일로드와 키 식별자가 복호화 없이 그대로 전달된다")
    void 잔액_조회_성공_시_암호화된_페일로드와_키_식별자가_그대로_전달된다() {
        // given
        String bankCode = "020";
        String bankKeyId = "test-bank-key-id";
        String reqPayload = "encrypted-jwe-payload-data";

        // SuperBuilder를 활용한 부모 필드 일괄 설정 (불변 객체 생성)
        BalanceInquiryRequest request = BalanceInquiryRequest.builder()
                .bankCode(bankCode)
                .reqPayload(reqPayload)
                .bankKeyId(bankKeyId)
                .build();

        BalanceInquiryResponse mockResponse = BalanceInquiryResponse.builder()
                .status("NORMAL")
                .build();

        given(bankExternalClient.fetchBalance(eq(bankCode), any(BankBalanceInquiryRequest.class)))
                .willReturn(mockResponse);

        // when
        BalanceInquiryResponse response = accountInquiryService.getBalance(request, bankKeyId);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo("NORMAL");

        // Zero-Knowledge 검증: Client로 넘어간 DTO의 암호화 필드가 훼손되거나 복호화되지 않고 원본 그대로 전달되었는지 검증
        ArgumentCaptor<BankBalanceInquiryRequest> captor = ArgumentCaptor.forClass(BankBalanceInquiryRequest.class);
        verify(bankExternalClient).fetchBalance(eq(bankCode), captor.capture());
        
        BankBalanceInquiryRequest capturedRequest = captor.getValue();
        assertThat(capturedRequest.getReqPayload()).isEqualTo(reqPayload);
        assertThat(capturedRequest.getBankKeyId()).isEqualTo(bankKeyId);
    }

    @Test
    @DisplayName("잔액 조회 시 외부 은행 서버 통신 실패하면 예외가 전파된다")
    void 잔액_조회_시_외부_은행_서버_통신_실패하면_예외가_전파된다() {
        // given
        String bankCode = "020";
        String bankKeyId = "test-bank-key-id";
        BalanceInquiryRequest request = BalanceInquiryRequest.builder()
                .bankCode(bankCode)
                .build();

        given(bankExternalClient.fetchBalance(eq(bankCode), any(BankBalanceInquiryRequest.class)))
                .willThrow(new BusinessException(ErrorCode.BANK_API_ERROR));

        // when & then
        assertThatThrownBy(() -> accountInquiryService.getBalance(request, bankKeyId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.BANK_API_ERROR);
    }

    /**
     * 거래 내역 조회 테스트 (getHistory)
     */
    @Test
    @DisplayName("거래 내역 조회 성공 시 암호화된 페일로드와 평문 파라미터가 올바르게 전달된다")
    void 거래_내역_조회_성공_시_암호화된_페일로드와_평문_파라미터가_올바르게_전달된다() {
        // given
        String bankCode = "020";
        String bankKeyId = "test-bank-key-id";
        String reqPayload = "encrypted-jwe-payload-data";
        String startDate = "2026-06-01";
        String endDate = "2026-06-10";
        int page = 0;
        int size = 20;

        // SuperBuilder를 활용한 부모 필드 일괄 설정 (불변 객체 생성)
        HistoryInquiryRequest request = HistoryInquiryRequest.builder()
                .bankCode(bankCode)
                .startDate(startDate)
                .endDate(endDate)
                .page(page)
                .size(size)
                .reqPayload(reqPayload)
                .bankKeyId(bankKeyId)
                .build();

        HistoryInquiryResponse mockResponse = HistoryInquiryResponse.builder()
                .totalCount(1)
                .build();

        given(bankExternalClient.fetchHistory(eq(bankCode), any(BankHistoryInquiryRequest.class)))
                .willReturn(mockResponse);

        // when
        HistoryInquiryResponse result = accountInquiryService.getHistory(request, bankKeyId);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getTotalCount()).isEqualTo(1);

        // Zero-Knowledge & 파라미터 전달 검증
        ArgumentCaptor<BankHistoryInquiryRequest> captor = ArgumentCaptor.forClass(BankHistoryInquiryRequest.class);
        verify(bankExternalClient).fetchHistory(eq(bankCode), captor.capture());

        BankHistoryInquiryRequest capturedRequest = captor.getValue();
        assertThat(capturedRequest.getReqPayload()).isEqualTo(reqPayload);
        assertThat(capturedRequest.getBankKeyId()).isEqualTo(bankKeyId);
        assertThat(capturedRequest.getStartDate()).isEqualTo(startDate);
        assertThat(capturedRequest.getEndDate()).isEqualTo(endDate);
        assertThat(capturedRequest.getPage()).isEqualTo(page);
        assertThat(capturedRequest.getSize()).isEqualTo(size);
    }

    @Test
    @DisplayName("조회 시작일과 종료일이 같으면 정상적으로 조회된다")
    void 조회_시작일과_종료일이_같으면_정상적으로_조회된다() {
        // given
        String bankCode = "020";
        String bankKeyId = "test-bank-key-id";
        String sameDate = "2026-06-10";

        HistoryInquiryRequest request = HistoryInquiryRequest.builder()
                .bankCode(bankCode)
                .startDate(sameDate)
                .endDate(sameDate)
                .page(0)
                .size(20)
                .build();

        HistoryInquiryResponse mockResponse = HistoryInquiryResponse.builder()
                .totalCount(0)
                .build();

        given(bankExternalClient.fetchHistory(eq(bankCode), any(BankHistoryInquiryRequest.class)))
                .willReturn(mockResponse);

        // when
        HistoryInquiryResponse result = accountInquiryService.getHistory(request, bankKeyId);

        // then
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("조회 시작일이 종료일보다 늦으면 예외가 발생한다")
    void 조회_시작일이_종료일보다_늦으면_예외가_발생한다() {
        // given
        HistoryInquiryRequest request = HistoryInquiryRequest.builder()
                .startDate("2026-06-10")
                .endDate("2026-06-09")
                .build();

        // when & then
        assertThatThrownBy(() -> accountInquiryService.getHistory(request, "test-bank-key-id"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INQUIRY_INVALID_DATE_RANGE);
    }

    @Test
    @DisplayName("시작일 날짜 형식이 올바르지 않으면 예외가 발생한다")
    void 시작일_날짜_형식이_올바르지_않으면_예외가_발생한다() {
        // given
        HistoryInquiryRequest request = HistoryInquiryRequest.builder()
                .startDate("20260610")
                .endDate("2026-06-11")
                .build();

        // when & then
        assertThatThrownBy(() -> accountInquiryService.getHistory(request, "test-bank-key-id"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT);
    }

    @Test
    @DisplayName("종료일 날짜 형식이 올바르지 않으면 예외가 발생한다")
    void 종료일_날짜_형식이_올바르지_않으면_예외가_발생한다() {
        // given
        HistoryInquiryRequest request = HistoryInquiryRequest.builder()
                .startDate("2026-06-10")
                .endDate("20260611")
                .build();

        // when & then
        assertThatThrownBy(() -> accountInquiryService.getHistory(request, "test-bank-key-id"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT);
    }

    @Test
    @DisplayName("거래 내역 조회 시 시분초가 포함된 날짜 형식은 예외가 발생한다")
    void 거래_내역_조회_시_시분초가_포함된_날짜_형식은_예외가_발생한다() {
        // given
        HistoryInquiryRequest request = HistoryInquiryRequest.builder()
                .startDate("2026-06-10 10:00:00")
                .endDate("2026-06-11 12:00:00")
                .build();

        // when & then
        assertThatThrownBy(() -> accountInquiryService.getHistory(request, "test-bank-key-id"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT);
    }

    @Test
    @DisplayName("존재하지 않는 은행 코드인 경우 예외가 발생한다")
    void 존재하지_않는_은행_코드인_경우_예외가_발생한다() {
        // given
        String invalidBankCode = "999";
        String bankKeyId = "test-bank-key-id";
        HistoryInquiryRequest request = HistoryInquiryRequest.builder()
                .bankCode(invalidBankCode)
                .startDate("2026-06-01")
                .endDate("2026-06-10")
                .page(0)
                .size(20)
                .build();

        given(bankExternalClient.fetchHistory(eq(invalidBankCode), any(BankHistoryInquiryRequest.class)))
                .willThrow(new BusinessException(ErrorCode.BANK_NOT_FOUND));

        // when & then
        assertThatThrownBy(() -> accountInquiryService.getHistory(request, bankKeyId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.BANK_NOT_FOUND);
    }

    @Test
    @DisplayName("올바른 날짜 범위 입력 시 예외가 발생하지 않는다")
    void 올바른_날짜_범위_입력_시_예외가_발생하지_않는다() {
        // given
        HistoryInquiryRequest request = HistoryInquiryRequest.builder()
                .startDate("2026-06-10")
                .endDate("2026-06-11")
                .build();

        // when & then
        // 이 메서드 실행 중 어떤 예외도 던져지지 않아야 성공함을 명시적으로 검증
        assertThatCode(() -> accountInquiryService.getHistory(request, "test-key-id"))
                .doesNotThrowAnyException();
    }
}

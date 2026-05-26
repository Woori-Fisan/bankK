package com.woorifisan.platform.domain.bank.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.woorifisan.platform.domain.bank.dto.request.BalanceInquiryRequest;
import com.woorifisan.platform.domain.bank.dto.request.HistoryInquiryRequest;
import com.woorifisan.platform.domain.bank.dto.response.BalanceInquiryResponse;
import com.woorifisan.platform.domain.bank.dto.response.HistoryInquiryResponse;
import com.woorifisan.platform.domain.bank.external.client.BankExternalClient;
import com.woorifisan.platform.domain.bank.external.dto.BankBalanceInquiryRequest;
import com.woorifisan.platform.global.exception.BusinessException;
import com.woorifisan.platform.global.response.ErrorCode;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AccountInquiryServiceTest {

    @Mock
    private BankExternalClient bankExternalClient;

    @InjectMocks
    private AccountInquiryService accountInquiryService;

    /**
     * 거래 내역 조회 Test (getHistory)
     */
    @Test
    @DisplayName("조회_시작일이_종료일보다_늦으면_예외가_발생한다")
    void 조회_시작일이_종료일보다_늦으면_예외가_발생한다() {
        // given
        HistoryInquiryRequest request = HistoryInquiryRequest.builder()
                .encryptedKey("testKey")
                .jwsSignature("testSignature")
                .bankCode("020")
                .accountNo("1234567890")
                .customerRrnPrefix("9001011")
                .startDate("2026-05-20")
                .endDate("2026-05-19") // 시작일이 종료일보다 늦음
                .page(1)
                .size(20)
                .build();

        // when & then
        assertThatThrownBy(() -> accountInquiryService.getHistory(request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INQUIRY_INVALID_DATE_RANGE);
    }

    @Test
    @DisplayName("정상적인_기간을_조회하면_거래내역을_반환한다")
    void 정상적인_기간을_조회하면_거래내역을_반환한다() {
        // given
        String startDateStr = "2026-05-19";
        String endDateStr = "2026-05-20";

        HistoryInquiryRequest request = HistoryInquiryRequest.builder()
                .encryptedKey("testKey")
                .jwsSignature("testSignature")
                .bankCode("020")
                .accountNo("1234567890")
                .customerRrnPrefix("9001011")
                .startDate(startDateStr)
                .endDate(endDateStr)
                .page(1)
                .size(20)
                .build();

        // when
        HistoryInquiryResponse response = accountInquiryService.getHistory(request);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getHistory()).isNotEmpty();

        // 더미 데이터 생성이므로 시작일과 종료일 범위 내에 있는지 대략적인 검증
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        LocalDate start = LocalDate.parse(startDateStr);
        LocalDate end = LocalDate.parse(endDateStr);

        response.getHistory().forEach(dto -> {
            LocalDate txDate = LocalDate.parse(dto.getTxDate(), formatter);
            assertThat(txDate).isBetween(start, end);
        });
    }

    @Test
    @DisplayName("계좌번호가_유효하지_않으면_예외가_발생한다")
    void 계좌번호가_유효하지_않으면_예외가_발생한다() {
        // given
        HistoryInquiryRequest request = HistoryInquiryRequest.builder()
                .encryptedKey("testKey")
                .jwsSignature("testSignature")
                .bankCode("020")
                .accountNo("0000000000") // INQUIRY_001 유발 더미 데이터
                .customerRrnPrefix("9001011")
                .startDate("2026-05-19")
                .endDate("2026-05-20")
                .page(1)
                .size(20)
                .build();

        // when & then
        assertThatThrownBy(() -> accountInquiryService.getHistory(request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INQUIRY_ACCOUNT_NOTFOUND);
    }

    @Test
    @DisplayName("은행API_호출중_오류가_발생하면_예외가_발생한다")
    void 은행API_호출중_오류가_발생하면_예외가_발생한다() {
        // given
        HistoryInquiryRequest request = HistoryInquiryRequest.builder()
                .encryptedKey("testKey")
                .jwsSignature("testSignature")
                .bankCode("999") // BANK_002 유발 더미 데이터
                .accountNo("1234567890")
                .customerRrnPrefix("9001011")
                .startDate("2026-05-19")
                .endDate("2026-05-20")
                .page(1)
                .size(20)
                .build();

        // when & then
        assertThatThrownBy(() -> accountInquiryService.getHistory(request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.BANK_API_ERROR);
    }


    /**
     * 잔액 조회 Test (getBalance)
     */
    @Test
    @DisplayName("잔액 조회 성공 케이스")
    void getBalance_Success() {
        // given
        BalanceInquiryRequest request = BalanceInquiryRequest.builder()
                .bankCode("020")
                .accountNo("123-456")
                .encryptedKey("encKey")
                .jwsSignature("signature")
                .customerRrnPrefix("9001014")
                .build();

        BalanceInquiryResponse expectedResponse = BalanceInquiryResponse.builder()
                .balance(new BigDecimal("1000000"))
                .status("NORMAL")
                .build();

        when(bankExternalClient.fetchBalance(eq("020"), any(BankBalanceInquiryRequest.class)))
                .thenReturn(expectedResponse);

        // when
        BalanceInquiryResponse actualResponse = accountInquiryService.getBalance(request);

        // then
        assertThat(actualResponse).isNotNull();
        assertThat(actualResponse.getBalance()).isEqualTo(new BigDecimal("1000000"));
        assertThat(actualResponse.getStatus()).isEqualTo("NORMAL");

        verify(bankExternalClient).fetchBalance(eq("020"), any(BankBalanceInquiryRequest.class));
    }

    @Test
    @DisplayName("은행 API 호출 중 오류 발생 시 BusinessException 발생")
    void getBalance_Fail_BankApiError() {
        // given
        BalanceInquiryRequest request = BalanceInquiryRequest.builder()
                .bankCode("020")
                .accountNo("123-456")
                .build();

        when(bankExternalClient.fetchBalance(any(), any()))
                .thenThrow(new BusinessException(ErrorCode.BANK_API_ERROR));

        // when & then
        assertThatThrownBy(() -> accountInquiryService.getBalance(request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.BANK_API_ERROR);
    }

    @Test
    @DisplayName("존재하지 않는 계좌 번호로 조회 시 BusinessException 발생")
    void getBalance_Fail_AccountNotFound() {
        // given
        BalanceInquiryRequest request = BalanceInquiryRequest.builder()
                .bankCode("020")
                .accountNo("0000000000") // validateAccountAndBank에서 체크하는 더미 값
                .build();

        // when & then
        assertThatThrownBy(() -> accountInquiryService.getBalance(request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INQUIRY_ACCOUNT_NOTFOUND);
    }
}

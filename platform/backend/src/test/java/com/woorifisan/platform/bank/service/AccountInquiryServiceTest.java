package com.woorifisan.platform.bank.service;

import com.woorifisan.platform.domain.bank.dto.request.HistoryInquiryRequest;
import com.woorifisan.platform.domain.bank.dto.response.HistoryInquiryResponse;
import com.woorifisan.platform.domain.bank.service.AccountInquiryService;
import com.woorifisan.platform.global.exception.BusinessException;
import com.woorifisan.platform.global.response.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class AccountInquiryServiceTest {

    @InjectMocks
    private AccountInquiryService accountInquiryService;

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
}

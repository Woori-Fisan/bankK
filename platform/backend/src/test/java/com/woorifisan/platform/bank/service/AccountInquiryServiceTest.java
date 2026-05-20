package com.woorifisan.platform.bank.service;

import com.woorifisan.platform.bank.dto.BalanceInquiryRequest;
import com.woorifisan.platform.bank.dto.BalanceInquiryResponse;
import com.woorifisan.platform.global.exception.BusinessException;
import com.woorifisan.platform.global.response.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class AccountInquiryServiceTest {

    @InjectMocks
    private AccountInquiryService accountInquiryService;

    @Test
    @DisplayName("잔액 조회 성공 테스트")
    void getBalance_success() {
        // given
        BalanceInquiryRequest request = BalanceInquiryRequest.builder()
                .accountNo("123456789")
                .bankCode("001")
                .customerRrnPrefix("9501011")
                .encryptedKey("key")
                .jwsSignature("sig")
                .build();

        // when
        BalanceInquiryResponse response = accountInquiryService.getBalance(request);

        // then
        assertThat(response.getBalance()).isEqualByComparingTo(new BigDecimal("5420000"));
        assertThat(response.getStatus()).isEqualTo("NORMAL");
    }

    @Test
    @DisplayName("잔액 조회 실패 테스트 - 계좌번호 0000")
    void getBalance_fail_invalid_account() {
        // given
        BalanceInquiryRequest request = BalanceInquiryRequest.builder()
                .accountNo("0000")
                .bankCode("001")
                .customerRrnPrefix("9501011")
                .encryptedKey("key")
                .jwsSignature("sig")
                .build();

        // when & then
        assertThatThrownBy(() -> accountInquiryService.getBalance(request))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> {
                    BusinessException be = (BusinessException) e;
                    assertThat(be.getErrorCode()).isEqualTo(ErrorCode.TRANSFER_DEPOSIT_ACCOUNT_FAULT);
                });
    }
}

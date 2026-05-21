package com.woorifisan.platform.bank.service;

import com.woorifisan.platform.domain.bank.dto.request.WithdrawalRequest;
import com.woorifisan.platform.domain.bank.dto.response.TransferResponse;
import com.woorifisan.platform.domain.bank.service.WithdrawalService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class WithdrawalServiceTest {

    @InjectMocks
    private WithdrawalService withdrawalService;

    @Test
    @DisplayName("출금 실행 성공 테스트 (Mock)")
    void executeWithdraw_success() {
        // given
        WithdrawalRequest request = new WithdrawalRequest();
        request.setWithdrawalAccountNo("11112222");
        request.setAmount(new BigDecimal("10000"));

        // when
        TransferResponse response = withdrawalService.executeWithdraw(request);

        // then
        assertThat(response.getTransactionId()).startsWith("TR-");
        assertThat(response.getBalanceAfter()).isEqualByComparingTo(new BigDecimal("113456"));
    }
}

package com.woorifisan.platform.bank.service;

import com.woorifisan.platform.bank.dto.TransferRecipientRequest;
import com.woorifisan.platform.bank.dto.TransferRecipientResponse;
import com.woorifisan.platform.bank.dto.TransferRequest;
import com.woorifisan.platform.bank.dto.TransferResponse;
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
class TransferServiceTest {

    @InjectMocks
    private TransferService transferService;

    @Test
    @DisplayName("수취인 조회 성공 테스트")
    void getRecipient_success() {
        // given
        TransferRecipientRequest request = new TransferRecipientRequest();
        request.setDepositBankCode("001");
        request.setDepositAccountNo("12345678");

        // when
        TransferRecipientResponse response = transferService.getRecipient(request);

        // then
        assertThat(response.getDepositorName()).isEqualTo("홍길동");
        assertThat(response.getDepositBankAccountNo()).isEqualTo("12345678");
    }

    @Test
    @DisplayName("수취인 조회 실패 테스트 - 계좌번호 0000")
    void getRecipient_fail_invalid_account() {
        // given
        TransferRecipientRequest request = new TransferRecipientRequest();
        request.setDepositBankCode("001");
        request.setDepositAccountNo("0000");

        // when & then
        assertThatThrownBy(() -> transferService.getRecipient(request))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> {
                    BusinessException be = (BusinessException) e;
                    assertThat(be.getErrorCode()).isEqualTo(ErrorCode.TRANSFER_DEPOSIT_ACCOUNT_FAULT);
                });
    }

    @Test
    @DisplayName("이체 실행 성공 테스트")
    void executeTransfer_success() {
        // given
        TransferRequest request = new TransferRequest();
        request.setWithdrawalAccountNo("11112222");
        request.setDepositAccountNo("33334444");
        request.setAmount(new BigDecimal("10000"));

        // when
        TransferResponse response = transferService.executeTransfer(request);

        // then
        assertThat(response.getTransactionId()).startsWith("TR-");
        assertThat(response.getBalanceAfter()).isEqualByComparingTo(new BigDecimal("5410000"));
    }
}

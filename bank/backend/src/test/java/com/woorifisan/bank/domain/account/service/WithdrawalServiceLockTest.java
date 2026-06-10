package com.woorifisan.bank.domain.account.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.woorifisan.bank.domain.account.dto.decrypted.DecryptedWithdrawData;
import com.woorifisan.bank.domain.account.dto.request.WithdrawalRequest;
import com.woorifisan.bank.domain.account.mapper.AccountMapper;
import com.woorifisan.bank.domain.account.mapper.TransactionLedgerMapper;
import com.woorifisan.bank.domain.account.model.Account;
import com.woorifisan.bank.domain.customer.mapper.CustomerMapper;
import com.woorifisan.bank.domain.customer.service.CustomerService;
import com.woorifisan.bank.global.exception.BusinessException;
import com.woorifisan.bank.global.response.ErrorCode;
import com.woorifisan.bank.global.security.service.SecurityService;
import java.math.BigDecimal;
import java.util.Optional;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class WithdrawalServiceLockTest {

    @InjectMocks
    private WithdrawalService withdrawalService;

    @Mock
    private AccountMapper accountMapper;

    @Mock
    private CustomerMapper customerMapper;

    @Mock
    private CustomerService customerService;

    @Mock
    private TransactionLedgerMapper transactionLedgerMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private SecurityService securityService;

    private void mockDecrypt(String accountNo, String rrnPrefix, String password) {
        DecryptedWithdrawData data = DecryptedWithdrawData.builder()
                .withdrawalAccountNo(accountNo)
                .customerRrnPrefix(rrnPrefix)
                .withdrawalPassword(password)
                .build();
        SecurityService.DecryptionResult<DecryptedWithdrawData> result =
                new SecurityService.DecryptionResult<>(data, mock(SecretKey.class));
        given(securityService.decryptWithKey(any(), eq(DecryptedWithdrawData.class)))
                .willReturn(result);
    }

    private WithdrawalRequest requestOf(BigDecimal amount) {
        return WithdrawalRequest.builder()
                .reqPayload("dummy-jwe")
                .bankKeyId("test-key")
                .amount(amount)
                .build();
    }

    @Nested
    @DisplayName("출금 테스트")
    class WithdrawalTests {

        @Test
        @DisplayName("실패: 출금 금액이 잔액보다 크면 사전 검증에서 INSUFFICIENT_BALANCE 예외가 발생해야 한다")
        void 출금_실패_사전검증_잔액부족() {
            // given
            mockDecrypt("acc-123", "900101", "password");

            Account account = Account.builder()
                    .id(1L)
                    .customerId(10L)
                    .accountNo("acc-123")
                    .password("encoded-password")
                    .balance(new BigDecimal("5000")) // 요청 금액 10000보다 작음
                    .accountType("DEPOSIT")
                    .status("NORMAL")
                    .build();

            given(accountMapper.findByAccountNoPlain("acc-123")).willReturn(Optional.of(account));
            given(passwordEncoder.matches(anyString(), anyString())).willReturn(true);

            // when & then
            assertThatThrownBy(() -> withdrawalService.withdraw(requestOf(new BigDecimal("10000"))))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INSUFFICIENT_BALANCE);
        }

    }
}
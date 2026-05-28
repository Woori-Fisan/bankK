package com.woorifisan.bank.domain.account.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

import com.woorifisan.bank.domain.account.dto.request.WithdrawalRequest;
import com.woorifisan.bank.domain.account.mapper.AccountMapper;
import com.woorifisan.bank.domain.account.mapper.TransactionLedgerMapper;
import com.woorifisan.bank.domain.account.model.Account;
import com.woorifisan.bank.global.exception.BusinessException;
import com.woorifisan.bank.global.response.ErrorCode;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
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
    private TransactionLedgerMapper transactionLedgerMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("실패: 업데이트 실패 시 잔액이 부족하면 INSUFFICIENT_BALANCE 예외가 발생해야 한다")
    void 출금_실패_업데이트시_잔액부족_구분() {
        // given
        BigDecimal amount = new BigDecimal("10000");
        WithdrawalRequest request = WithdrawalRequest.builder()
                .withdrawalAccountNo("123-456")
                .customerRrnPrefix("900101")
                .withdrawalPassword("password")
                .amount(amount)
                .build();

        Account account = Account.builder()
                .id(1L)
                .accountNo("123-456")
                .password("encoded-password")
                .balance(new BigDecimal("20000"))
                .accountType("DEPOSIT")
                .status("NORMAL")
                .version(1)
                .build();

        given(accountMapper.findByAccountNoAndRrnPrefix(anyString(), anyString()))
                .willReturn(Optional.of(account));
        given(passwordEncoder.matches(anyString(), anyString())).willReturn(true);
        
        // updateBalance 0을 반환하도록 설정 (실패 상황)
        given(accountMapper.updateBalance(anyLong(), any(BigDecimal.class), anyInt()))
                .willReturn(0);
        
        // 다시 조회했을 때 잔액이 부족한 상황 모사
        Account updatedAccount = Account.builder()
                .id(1L)
                .balance(new BigDecimal("5000")) // 요청 금액 10000보다 작음
                .version(2)
                .build();
        given(accountMapper.findById(1L)).willReturn(Optional.of(updatedAccount));

        // when & then
        assertThatThrownBy(() -> withdrawalService.withdraw(request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INSUFFICIENT_BALANCE);
    }

    @Test
    @DisplayName("실패: 업데이트 실패 시 잔액은 충분하지만 버전이 다르면 CONCURRENT_MODIFICATION 예외가 발생해야 한다")
    void 출금_실패_업데이트시_버전불일치_구분() {
        // given
        BigDecimal amount = new BigDecimal("10000");
        WithdrawalRequest request = WithdrawalRequest.builder()
                .withdrawalAccountNo("123-456")
                .customerRrnPrefix("900101")
                .withdrawalPassword("password")
                .amount(amount)
                .build();

        Account account = Account.builder()
                .id(1L)
                .accountNo("123-456")
                .password("encoded-password")
                .balance(new BigDecimal("20000"))
                .accountType("DEPOSIT")
                .status("NORMAL")
                .version(1)
                .build();

        given(accountMapper.findByAccountNoAndRrnPrefix(anyString(), anyString()))
                .willReturn(Optional.of(account));
        given(passwordEncoder.matches(anyString(), anyString())).willReturn(true);
        
        // subtractBalance가 0을 반환하도록 설정
        given(accountMapper.updateBalance(anyLong(), any(BigDecimal.class), anyInt()))
                .willReturn(0);
        
        // 다시 조회했을 때 잔액은 충분하지만 버전이 다른 상황 모사
        Account updatedAccount = Account.builder()
                .id(1L)
                .balance(new BigDecimal("20000")) 
                .version(2) // 버전이 올라감
                .build();
        given(accountMapper.findById(1L)).willReturn(Optional.of(updatedAccount));

        // when & then
        assertThatThrownBy(() -> withdrawalService.withdraw(request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CONCURRENT_MODIFICATION);
    }
}

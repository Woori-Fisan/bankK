package com.woorifisan.bank.domain.account.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.woorifisan.bank.domain.account.dto.request.WithdrawalRequest;
import com.woorifisan.bank.domain.account.dto.response.WithdrawalResponse;
import com.woorifisan.bank.domain.account.mapper.AccountMapper;
import com.woorifisan.bank.domain.account.model.Account;
import com.woorifisan.bank.global.exception.BusinessException;
import com.woorifisan.bank.global.response.ErrorCode;
import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@Sql("/sql/withdrawal-service-test.sql")
class WithdrawalServiceTest {

    @Autowired
    private WithdrawalService withdrawalService;

    @Autowired
    private AccountMapper accountMapper;

    @Test
    @DisplayName("성공: 모든 조건이 충족되면 출금이 완료되고 잔액이 차감된다")
    void 출금_성공() {
        // given
        BigDecimal withdrawAmount = new BigDecimal("30000.00");
        WithdrawalRequest request = WithdrawalRequest.builder()
                .withdrawalAccountNo("acc-hash-100")
                .customerRrnPrefix("rrn-100")
                .withdrawalPassword("123456")
                .amount(withdrawAmount)
                .build();

        // when
        WithdrawalResponse response = withdrawalService.withdraw(request);

        // then
        assertThat(response.getTransactionId()).startsWith("TXW-");
        assertThat(response.getBalanceAfter()).isEqualByComparingTo(new BigDecimal("70000.00"));
        
        // DB 잔액 확인
        Account updatedAccount = accountMapper.findByAccountNoHashAndRrnPrefix("acc-hash-100", "rrn-100").get();
        assertThat(updatedAccount.getBalance()).isEqualByComparingTo(new BigDecimal("70000.00"));
    }

    @Test
    @DisplayName("실패: 비밀번호 해시가 틀리면 BANK_PW_ERROR 예외가 발생한다")
    void 출금_실패_비밀번호불일치() {
        // given
        WithdrawalRequest request = WithdrawalRequest.builder()
                .withdrawalAccountNo("acc-hash-100")
                .customerRrnPrefix("rrn-100")
                .withdrawalPassword("5678")
                .amount(new BigDecimal("1000.00"))
                .build();

        // when & then
        assertThatThrownBy(() -> withdrawalService.withdraw(request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ACCOUNT_PW_ERROR);
    }

    @Test
    @DisplayName("실패: 잔액이 부족하면 INSUFFICIENT_BALANCE 예외가 발생한다")
    void 출금_실패_잔액부족() {
        // given
        WithdrawalRequest request = WithdrawalRequest.builder()
                .withdrawalAccountNo("acc-hash-101")
                .customerRrnPrefix("rrn-101")
                .withdrawalPassword("123456")
                .amount(new BigDecimal("5000.00")) // 잔액은 1000
                .build();

        // when & then
        assertThatThrownBy(() -> withdrawalService.withdraw(request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INSUFFICIENT_BALANCE);
    }

    @Test
    @DisplayName("실패: 계좌가 잠금 상태면 ACCOUNT_NOT_NORMAL 예외가 발생한다")
    void 출금_실패_계좌잠금() {
        // given
        WithdrawalRequest request = WithdrawalRequest.builder()
                .withdrawalAccountNo("acc-hash-102")
                .customerRrnPrefix("rrn-102")
                .withdrawalPassword("123456")
                .amount(new BigDecimal("1000.00"))
                .build();

        // when & then
        assertThatThrownBy(() -> withdrawalService.withdraw(request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ACCOUNT_NOT_NORMAL);
    }

    @Test
    @DisplayName("실패: 출금 불가능한 계좌 유형이면 INVALID_ACCOUNT_TYPE 예외가 발생한다")
    void 출금_실패_계좌유형부적합() {
        // given
        WithdrawalRequest request = WithdrawalRequest.builder()
                .withdrawalAccountNo("acc-hash-103")
                .customerRrnPrefix("rrn-103")
                .withdrawalPassword("123456")
                .amount(new BigDecimal("1000.00"))
                .build();

        // when & then
        assertThatThrownBy(() -> withdrawalService.withdraw(request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_ACCOUNT_TYPE);
    }
}

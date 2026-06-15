package com.woorifisan.bank.domain.account.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.woorifisan.bank.domain.account.dto.decrypted.DecryptedWithdrawData;
import com.woorifisan.bank.domain.account.dto.request.WithdrawalRequest;
import com.woorifisan.bank.domain.account.dto.response.WithdrawalResponse;
import com.woorifisan.bank.domain.account.mapper.AccountMapper;
import com.woorifisan.bank.domain.account.mapper.TransactionLedgerMapper;
import com.woorifisan.bank.domain.account.model.Account;
import com.woorifisan.bank.domain.customer.service.CustomerService;
import com.woorifisan.bank.global.exception.BusinessException;
import com.woorifisan.bank.global.response.ErrorCode;
import com.woorifisan.bank.global.security.service.SecurityService;
import com.woorifisan.bank.global.util.CryptoUtil;
import java.math.BigDecimal;
import java.util.Optional;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class WithdrawalServiceTest {

    @InjectMocks
    private WithdrawalService withdrawalService;

    @Mock
    private AccountMapper accountMapper;

    @Mock
    private CustomerService customerService;

    @Mock
    private TransactionLedgerMapper transactionLedgerMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private SecurityService securityService;

    @Mock
    private CryptoUtil cryptoUtil;


    private void mockDecrypt(String accountNo, String rrnPrefix, String password, String customerName) {
        DecryptedWithdrawData data = DecryptedWithdrawData.builder()
                .withdrawalAccountNo(accountNo)
                .customerRrnPrefix(rrnPrefix)
                .withdrawalPassword(password)
                .customerName(customerName)
                .build();
        SecurityService.DecryptionResult<DecryptedWithdrawData> result =
                new SecurityService.DecryptionResult<>(data, mock(SecretKey.class));
        given(securityService.decryptWithKey(any(), eq(DecryptedWithdrawData.class)))
                .willReturn(result);
    }

    private void mockAccount(String accountNo, Account account) {
        String hash = "hash-" + accountNo;
        given(cryptoUtil.hash(accountNo)).willReturn(hash);
        given(accountMapper.findByAccountNoHash(hash)).willReturn(Optional.of(account));
    }

    private WithdrawalRequest requestOf(BigDecimal amount) {
        return WithdrawalRequest.builder()
                .reqPayload("dummy-jwe")
                .bankKeyId("test-key")
                .amount(amount)
                .build();
    }

    @Test
    @DisplayName("성공: 모든 조건이 충족되면 출금이 완료되고 잔액이 차감된다")
    void 출금_성공() {
        // given
        Account account = Account.builder()
                .id(100L).customerId(100L)
                .accountType("DEPOSIT").balance(new BigDecimal("100000.00")).status("NORMAL")
                .password("hashed-pw")
                .build();
        mockDecrypt("acc-100", "rrn-100", "123456", "성공자");
        mockAccount("acc-100", account);
        given(passwordEncoder.matches("123456", "hashed-pw")).willReturn(true);
        given(accountMapper.findByIdForUpdate(100L)).willReturn(Optional.of(account));
        given(securityService.encryptResponse(any(), any(SecretKey.class))).willReturn("encrypted-payload");

        // when
        WithdrawalResponse response = withdrawalService.withdraw(requestOf(new BigDecimal("30000.00")));

        // then
        assertThat(response.getTransactionId()).startsWith("TXW-");
        assertThat(response.getBalanceAfter()).isEqualByComparingTo(new BigDecimal("70000.00"));
        verify(accountMapper).updateBalance(100L, new BigDecimal("30000.00").negate());
    }

    @Test
    @DisplayName("실패: 비밀번호 해시가 틀리면 ACCOUNT_PW_ERROR 예외가 발생한다")
    void 출금_실패_비밀번호불일치() {
        // given
        Account account = Account.builder()
                .id(100L).customerId(100L)
                .accountType("DEPOSIT").balance(new BigDecimal("100000.00")).status("NORMAL")
                .password("hashed-pw")
                .build();
        mockDecrypt("acc-100", "rrn-100", "5678", "성공자");
        mockAccount("acc-100", account);
        given(passwordEncoder.matches("5678", "hashed-pw")).willReturn(false);

        // when & then
        assertThatThrownBy(() -> withdrawalService.withdraw(requestOf(new BigDecimal("1000.00"))))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ACCOUNT_PW_ERROR);
    }

    @Test
    @DisplayName("실패: 잔액이 부족하면 INSUFFICIENT_BALANCE 예외가 발생한다")
    void 출금_실패_잔액부족() {
        // given
        Account account = Account.builder()
                .id(101L).customerId(101L)
                .accountType("DEPOSIT").balance(new BigDecimal("1000.00")).status("NORMAL")
                .password("hashed-pw")
                .build();
        mockDecrypt("acc-101", "rrn-101", "123456", "가난뱅이");
        mockAccount("acc-101", account);
        given(passwordEncoder.matches("123456", "hashed-pw")).willReturn(true);

        // when & then
        assertThatThrownBy(() -> withdrawalService.withdraw(requestOf(new BigDecimal("5000.00"))))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INSUFFICIENT_BALANCE);
    }

    @Test
    @DisplayName("실패: 계좌가 잠금 상태면 ACCOUNT_NOT_NORMAL 예외가 발생한다")
    void 출금_실패_계좌잠금() {
        // given
        Account account = Account.builder()
                .id(102L).customerId(102L)
                .accountType("DEPOSIT").balance(new BigDecimal("50000.00")).status("LOCKED")
                .password("hashed-pw")
                .build();
        mockDecrypt("acc-102", "rrn-102", "123456", "잠긴자");
        mockAccount("acc-102", account);
        // validateWithdrawal은 상태 체크 후 예외를 던지므로 passwordEncoder는 호출되지 않음

        // when & then
        assertThatThrownBy(() -> withdrawalService.withdraw(requestOf(new BigDecimal("1000.00"))))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ACCOUNT_NOT_NORMAL);
    }

    @Test
    @DisplayName("실패: 출금 불가능한 계좌 유형이면 INVALID_ACCOUNT_TYPE 예외가 발생한다")
    void 출금_실패_계좌유형부적합() {
        // given
        Account account = Account.builder()
                .id(103L).customerId(103L)
                .accountType("LOAN").balance(new BigDecimal("50000.00")).status("NORMAL")
                .password("hashed-pw")
                .build();
        mockDecrypt("acc-103", "rrn-103", "123456", "대출자");
        mockAccount("acc-103", account);
        // validateWithdrawal은 타입 체크에서 즉시 예외를 던지므로 passwordEncoder는 호출되지 않음

        // when & then
        assertThatThrownBy(() -> withdrawalService.withdraw(requestOf(new BigDecimal("1000.00"))))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_ACCOUNT_TYPE);
    }

    // ── 비관적 락 재조회(findByIdForUpdate) 이후 재검증 실패 케이스 ──────────────
    // 사전 검증은 통과했지만, 락 획득 사이에 다른 트랜잭션이 계좌 상태를 변경한 상황을 재현한다.

    @Test
    @DisplayName("실패(락 재조회): 락 획득 후 계좌 유형이 LOAN으로 변경됐으면 INVALID_ACCOUNT_TYPE 예외가 발생한다")
    void 출금_실패_락재조회_계좌유형변경() {
        // given: acc-100은 사전 검증 통과 (DEPOSIT, NORMAL, 잔액 100,000, 비밀번호 일치)
        Account originalAccount = Account.builder()
                .id(100L).customerId(100L)
                .accountType("DEPOSIT").balance(new BigDecimal("100000.00")).status("NORMAL")
                .password("hashed-pw")
                .build();
        mockDecrypt("acc-100", "rrn-100", "123456", "성공자");
        mockAccount("acc-100", originalAccount);
        given(passwordEncoder.matches("123456", "hashed-pw")).willReturn(true);

        // 락 획득 전에 다른 트랜잭션이 계좌 유형을 LOAN으로 변경했다고 가정
        Account changedAccount = Account.builder()
                .id(100L).customerId(100L).accountType("LOAN")
                .balance(new BigDecimal("100000.00")).status("NORMAL")
                .build();
        given(accountMapper.findByIdForUpdate(100L)).willReturn(Optional.of(changedAccount));

        // when & then
        assertThatThrownBy(() -> withdrawalService.withdraw(requestOf(new BigDecimal("30000.00"))))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_ACCOUNT_TYPE);
    }

    @Test
    @DisplayName("실패(락 재조회): 락 획득 후 계좌 상태가 LOCKED로 변경됐으면 ACCOUNT_NOT_NORMAL 예외가 발생한다")
    void 출금_실패_락재조회_계좌상태변경() {
        // given: acc-100은 사전 검증 통과
        Account originalAccount = Account.builder()
                .id(100L).customerId(100L)
                .accountType("DEPOSIT").balance(new BigDecimal("100000.00")).status("NORMAL")
                .password("hashed-pw")
                .build();
        mockDecrypt("acc-100", "rrn-100", "123456", "성공자");
        mockAccount("acc-100", originalAccount);
        given(passwordEncoder.matches("123456", "hashed-pw")).willReturn(true);

        // 락 획득 전에 다른 트랜잭션이 계좌를 잠금 처리했다고 가정
        Account changedAccount = Account.builder()
                .id(100L).customerId(100L).accountType("DEPOSIT")
                .balance(new BigDecimal("100000.00")).status("LOCKED")
                .build();
        given(accountMapper.findByIdForUpdate(100L)).willReturn(Optional.of(changedAccount));

        // when & then
        assertThatThrownBy(() -> withdrawalService.withdraw(requestOf(new BigDecimal("30000.00"))))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ACCOUNT_NOT_NORMAL);
    }

    @Test
    @DisplayName("실패(락 재조회): 락 획득 후 잔액이 출금액보다 적어졌으면 INSUFFICIENT_BALANCE 예외가 발생한다")
    void 출금_실패_락재조회_잔액감소() {
        // given: acc-100은 사전 검증 통과 (잔액 100,000 → 30,000 출금 요청이므로 통과)
        Account originalAccount = Account.builder()
                .id(100L).customerId(100L)
                .accountType("DEPOSIT").balance(new BigDecimal("100000.00")).status("NORMAL")
                .password("hashed-pw")
                .build();
        mockDecrypt("acc-100", "rrn-100", "123456", "성공자");
        mockAccount("acc-100", originalAccount);
        given(passwordEncoder.matches("123456", "hashed-pw")).willReturn(true);

        // 락 획득 전에 다른 트랜잭션이 먼저 출금해 잔액이 5,000원으로 줄었다고 가정
        Account changedAccount = Account.builder()
                .id(100L).customerId(100L).accountType("DEPOSIT")
                .balance(new BigDecimal("5000.00")).status("NORMAL")
                .build();
        given(accountMapper.findByIdForUpdate(100L)).willReturn(Optional.of(changedAccount));

        // when & then
        assertThatThrownBy(() -> withdrawalService.withdraw(requestOf(new BigDecimal("30000.00"))))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INSUFFICIENT_BALANCE);
    }
}
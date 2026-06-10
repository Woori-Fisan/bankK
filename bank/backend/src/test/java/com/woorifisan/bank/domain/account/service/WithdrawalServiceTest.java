package com.woorifisan.bank.domain.account.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import com.woorifisan.bank.domain.account.dto.decrypted.DecryptedWithdrawData;
import com.woorifisan.bank.domain.account.dto.request.WithdrawalRequest;
import com.woorifisan.bank.domain.account.dto.response.WithdrawalResponse;
import com.woorifisan.bank.domain.account.mapper.AccountMapper;
import com.woorifisan.bank.domain.account.model.Account;
import com.woorifisan.bank.global.exception.BusinessException;
import com.woorifisan.bank.global.response.ErrorCode;
import com.woorifisan.bank.global.security.service.SecurityService;
import java.math.BigDecimal;
import java.util.Optional;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlConfig;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@Sql(scripts = "/sql/withdrawal-service-test.sql", config = @SqlConfig(encoding = "UTF-8"))
class WithdrawalServiceTest {

    @Autowired
    private WithdrawalService withdrawalService;

    @MockitoSpyBean
    private AccountMapper accountMapper;

    @MockitoBean
    private SecurityService securityService;

    @BeforeEach
    void setUp() {
        given(securityService.encryptResponse(any(), any(SecretKey.class)))
                .willReturn("encrypted-payload");
    }

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
        mockDecrypt("acc-100", "rrn-100", "123456", "성공자");

        // when
        WithdrawalResponse response = withdrawalService.withdraw(requestOf(new BigDecimal("30000.00")));

        // then
        assertThat(response.getTransactionId()).startsWith("TXW-");
        assertThat(response.getBalanceAfter()).isEqualByComparingTo(new BigDecimal("70000.00"));

        // DB 잔액 확인
        Account updatedAccount = accountMapper.findByAccountNoPlain("acc-100").get();
        assertThat(updatedAccount.getBalance()).isEqualByComparingTo(new BigDecimal("70000.00"));
    }

    @Test
    @DisplayName("실패: 비밀번호 해시가 틀리면 ACCOUNT_PW_ERROR 예외가 발생한다")
    void 출금_실패_비밀번호불일치() {
        // given
        mockDecrypt("acc-100", "rrn-100", "5678", "성공자");

        // when & then
        assertThatThrownBy(() -> withdrawalService.withdraw(requestOf(new BigDecimal("1000.00"))))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ACCOUNT_PW_ERROR);
    }

    @Test
    @DisplayName("실패: 잔액이 부족하면 INSUFFICIENT_BALANCE 예외가 발생한다")
    void 출금_실패_잔액부족() {
        // given
        mockDecrypt("acc-101", "rrn-101", "123456", "가난뱅이");

        // when & then
        assertThatThrownBy(() -> withdrawalService.withdraw(requestOf(new BigDecimal("5000.00"))))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INSUFFICIENT_BALANCE);
    }

    @Test
    @DisplayName("실패: 계좌가 잠금 상태면 ACCOUNT_NOT_NORMAL 예외가 발생한다")
    void 출금_실패_계좌잠금() {
        // given
        mockDecrypt("acc-102", "rrn-102", "123456", "잠긴자");

        // when & then
        assertThatThrownBy(() -> withdrawalService.withdraw(requestOf(new BigDecimal("1000.00"))))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ACCOUNT_NOT_NORMAL);
    }

    @Test
    @DisplayName("실패: 출금 불가능한 계좌 유형이면 INVALID_ACCOUNT_TYPE 예외가 발생한다")
    void 출금_실패_계좌유형부적합() {
        // given
        mockDecrypt("acc-103", "rrn-103", "123456", "대출자");

        // when & then
        assertThatThrownBy(() -> withdrawalService.withdraw(requestOf(new BigDecimal("1000.00"))))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_ACCOUNT_TYPE);
    }

    // ── 비관적 락 재조회(findByIdForUpdate) 이후 재검증 실패 케이스 ──────────────
    // 사전 검증은 통과했지만, 락 획득 사이에 다른 트랜잭션이 계좌 상태를 변경한 상황을 재현한다.
    // AccountMapper를 @MockitoSpyBean으로 주입해 findByIdForUpdate만 스텁한다.

    @Test
    @DisplayName("실패(락 재조회): 락 획득 후 계좌 유형이 LOAN으로 변경됐으면 INVALID_ACCOUNT_TYPE 예외가 발생한다")
    void 출금_실패_락재조회_계좌유형변경() {
        // given: acc-100은 사전 검증 통과 (DEPOSIT, NORMAL, 잔액 100,000, 비밀번호 일치)
        mockDecrypt("acc-100", "rrn-100", "123456", "성공자");

        // 락 획득 전에 다른 트랜잭션이 계좌 유형을 LOAN으로 변경했다고 가정
        Account changedAccount = Account.builder()
                .id(100L).customerId(100L).accountNo("acc-100")
                .accountType("LOAN")           // 변경됨
                .balance(new BigDecimal("100000.00"))
                .status("NORMAL")
                .build();
        doReturn(Optional.of(changedAccount)).when(accountMapper).findByIdForUpdate(100L);

        // when & then
        assertThatThrownBy(() -> withdrawalService.withdraw(requestOf(new BigDecimal("30000.00"))))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_ACCOUNT_TYPE);
    }

    @Test
    @DisplayName("실패(락 재조회): 락 획득 후 계좌 상태가 LOCKED로 변경됐으면 ACCOUNT_NOT_NORMAL 예외가 발생한다")
    void 출금_실패_락재조회_계좌상태변경() {
        // given: acc-100은 사전 검증 통과
        mockDecrypt("acc-100", "rrn-100", "123456", "성공자");

        // 락 획득 전에 다른 트랜잭션이 계좌를 잠금 처리했다고 가정
        Account changedAccount = Account.builder()
                .id(100L).customerId(100L).accountNo("acc-100")
                .accountType("DEPOSIT")
                .balance(new BigDecimal("100000.00"))
                .status("LOCKED")              // 변경됨
                .build();
        doReturn(Optional.of(changedAccount)).when(accountMapper).findByIdForUpdate(100L);

        // when & then
        assertThatThrownBy(() -> withdrawalService.withdraw(requestOf(new BigDecimal("30000.00"))))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ACCOUNT_NOT_NORMAL);
    }

    @Test
    @DisplayName("실패(락 재조회): 락 획득 후 잔액이 출금액보다 적어졌으면 INSUFFICIENT_BALANCE 예외가 발생한다")
    void 출금_실패_락재조회_잔액감소() {
        // given: acc-100은 사전 검증 통과 (잔액 100,000 → 30,000 출금 요청이므로 통과)
        mockDecrypt("acc-100", "rrn-100", "123456", "성공자");

        // 락 획득 전에 다른 트랜잭션이 먼저 출금해 잔액이 5,000원으로 줄었다고 가정
        Account changedAccount = Account.builder()
                .id(100L).customerId(100L).accountNo("acc-100")
                .accountType("DEPOSIT")
                .balance(new BigDecimal("5000.00")) // 잔액 감소
                .status("NORMAL")
                .build();
        doReturn(Optional.of(changedAccount)).when(accountMapper).findByIdForUpdate(100L);

        // when & then
        assertThatThrownBy(() -> withdrawalService.withdraw(requestOf(new BigDecimal("30000.00"))))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INSUFFICIENT_BALANCE);
    }
}
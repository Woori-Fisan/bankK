package com.woorifisan.bank.domain.account.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
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
import javax.crypto.SecretKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
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

    @Autowired
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
}
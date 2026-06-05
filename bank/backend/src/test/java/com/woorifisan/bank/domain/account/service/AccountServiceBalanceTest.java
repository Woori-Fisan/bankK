package com.woorifisan.bank.domain.account.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.woorifisan.bank.domain.account.dto.decrypted.DecryptedInquiryData;
import com.woorifisan.bank.domain.account.dto.request.BalanceInquiryRequest;
import com.woorifisan.bank.domain.account.dto.response.BalanceInquiryResponse;
import com.woorifisan.bank.global.exception.BusinessException;
import com.woorifisan.bank.global.response.ErrorCode;
import com.woorifisan.bank.global.security.service.SecurityService;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@Sql("/sql/account-service-test.sql")
public class AccountServiceBalanceTest {

    @Autowired
    private AccountService accountService;

    @MockitoBean
    private SecurityService securityService;

    @BeforeEach
    void setUp() {
        given(securityService.encryptResponse(any(), any(SecretKey.class)))
                .willReturn("encrypted-payload");
    }

    private void mockDecrypt(String accountNo, String rrnPrefix) {
        DecryptedInquiryData data = new DecryptedInquiryData();
        data.setAccountNo(accountNo);
        data.setCustomerRrnPrefix(rrnPrefix);
        SecurityService.DecryptionResult<DecryptedInquiryData> result =
                new SecurityService.DecryptionResult<>(data, mock(SecretKey.class));
        given(securityService.decryptWithKey(any(), eq(DecryptedInquiryData.class)))
                .willReturn(result);
    }

    private BalanceInquiryRequest dummyRequest() {
        return BalanceInquiryRequest.builder()
                .reqPayload("dummy-jwe")
                .bankKeyId("test-key")
                .build();
    }

    @Test
    @DisplayName("성공: 계좌번호와 주민번호 앞자리가 일치하면 잔액을 조회한다")
    void 잔액조회_성공() {
        // given
        mockDecrypt("111-222-3333", "9501014");

        // when
        BalanceInquiryResponse response = accountService.getBalance(dummyRequest());

        // then
        assertThat(response.getStatus()).isEqualTo("NORMAL");
        assertThat(response.getResPayload()).isEqualTo("encrypted-payload");
    }

    @Test
    @DisplayName("실패: 계좌번호가 일치하지 않으면 ACCOUNT_NOT_FOUND 예외가 발생한다")
    void 잔액조회_실패_계좌번호불일치() {
        // given
        mockDecrypt("wrong-acc", "9501014");

        // when & then
        assertThatThrownBy(() -> accountService.getBalance(dummyRequest()))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ACCOUNT_NOT_FOUND);
    }

    @Test
    @DisplayName("실패: 주민번호 앞자리가 일치하지 않으면 USER_NOT_FOUND 예외가 발생한다")
    void 잔액조회_실패_주민번호불일치() {
        // given
        mockDecrypt("111-222-3333", "wrong-rrn");

        // when & then
        assertThatThrownBy(() -> accountService.getBalance(dummyRequest()))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);
    }
}
package com.woorifisan.bank.domain.account.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.woorifisan.bank.domain.account.dto.request.BalanceInquiryRequest;
import com.woorifisan.bank.domain.account.dto.response.BalanceInquiryResponse;
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
@Transactional // 각 테스트 후 롤백되어 데이터 정합성 유지
@Sql("/sql/account-service-test.sql") // 테스트 데이터 삽입
public class AccountServiceBalanceTest {
    @Autowired
    private AccountService accountService;

    @Test
    @DisplayName("성공: 계좌번호와 주민번호 앞자리가 일치하면 잔액을 조회한다")
    void 잔액조회_성공() {
        // given
        BalanceInquiryRequest request = BalanceInquiryRequest.builder()
                .accountNo("111-222-3333")
                .customerRrnPrefix("9501014")
                .build();

        // when
        BalanceInquiryResponse response = accountService.getBalance(request);

        // then
        assertThat(response.getBalance()).isEqualByComparingTo(new BigDecimal("150000.00"));
        assertThat(response.getStatus()).isEqualTo("NORMAL");
    }

    @Test
    @DisplayName("실패: 계좌번호가 일치하지 않으면 ACCOUNT_NOT_FOUND 예외가 발생한다")
    void 잔액조회_실패_계좌번호불일치() {
        // given
        BalanceInquiryRequest request = BalanceInquiryRequest.builder()
                .accountNo("wrong-acc")
                .customerRrnPrefix("9501014")
                .build();

        // when & then
        assertThatThrownBy(() -> accountService.getBalance(request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ACCOUNT_NOT_FOUND);
    }

    @Test
    @DisplayName("실패: 주민번호 앞자리가 일치하지 않으면 ACCOUNT_NOT_FOUND 예외가 발생한다")
    void 잔액조회_실패_주민번호불일치() {
        // given
        BalanceInquiryRequest request = BalanceInquiryRequest.builder()
                .accountNo("111-222-3333")
                .customerRrnPrefix("wrong-rrn-prefix")
                .build();

        // when & then
        assertThatThrownBy(() -> accountService.getBalance(request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ACCOUNT_NOT_FOUND);
    }
}

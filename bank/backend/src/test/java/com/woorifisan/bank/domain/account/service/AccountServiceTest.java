package com.woorifisan.bank.domain.account.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.woorifisan.bank.domain.account.dto.decrypted.DecryptedInquiryData;
import com.woorifisan.bank.domain.account.dto.request.TransactionHistoryRequest;
import com.woorifisan.bank.domain.account.dto.response.TransactionHistoryResponse;
import com.woorifisan.bank.domain.account.mapper.AccountMapper;
import com.woorifisan.bank.domain.account.mapper.TransactionLedgerMapper;
import com.woorifisan.bank.domain.account.model.Account;
import com.woorifisan.bank.domain.customer.mapper.CustomerMapper;
import com.woorifisan.bank.domain.customer.model.Customer;
import com.woorifisan.bank.global.exception.BusinessException;
import com.woorifisan.bank.global.response.ErrorCode;
import com.woorifisan.bank.global.security.service.SecurityService;
import java.util.List;
import java.util.Optional;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @InjectMocks
    private AccountService accountService;

    @Mock
    private AccountMapper accountMapper;

    @Mock
    private TransactionLedgerMapper transactionLedgerMapper;

    @Mock
    private CustomerMapper customerMapper;

    @Mock
    private SecurityService securityService;

    private void mockDecrypt(String accountNo, String rrnPrefix) {
        DecryptedInquiryData data = new DecryptedInquiryData();
        data.setAccountNo(accountNo);
        data.setCustomerRrnPrefix(rrnPrefix);
        SecurityService.DecryptionResult<DecryptedInquiryData> result =
                new SecurityService.DecryptionResult<>(data, mock(SecretKey.class));
        given(securityService.decryptWithKey(any(), eq(DecryptedInquiryData.class)))
                .willReturn(result);
    }

    private TransactionHistoryRequest requestOf(String startDate, String endDate) {
        return TransactionHistoryRequest.builder()
                .reqPayload("dummy-jwe")
                .bankKeyId("test-key")
                .startDate(startDate)
                .endDate(endDate)
                .page(0)
                .size(10)
                .build();
    }

    @Nested
    @DisplayName("거래 내역 조회 테스트")
    class GetTransactionHistoryList {

        @BeforeEach
        void setUp() {
            given(securityService.encryptResponse(any(), any(SecretKey.class)))
                    .willReturn("encrypted-payload");
        }

        @Test
        @DisplayName("성공: 유효한 계좌와 본인 정보로 거래 내역을 조회할 수 있다")
        void 성공_거래내역조회() {
            // given
            mockDecrypt("acc-123", "900101");

            Account account = Account.builder()
                    .id(1L)
                    .customerId(10L)
                    .accountNo("acc-123")
                    .build();

            Customer customer = Customer.builder()
                    .id(10L)
                    .rrnPrefix("900101")
                    .build();

            given(accountMapper.findByAccountNoPlain("acc-123")).willReturn(Optional.of(account));
            given(customerMapper.findById(10L)).willReturn(Optional.of(customer));
            given(transactionLedgerMapper.countHistory(anyLong(), anyString(), anyString())).willReturn(5);
            given(transactionLedgerMapper.findHistoryList(anyLong(), anyString(), anyString(), anyInt(), anyInt()))
                    .willReturn(List.of());

            // when
            TransactionHistoryResponse response = accountService.getTransactionHistoryList(
                    requestOf("2024-01-01", "2024-01-31"));

            // then
            assertThat(response.getTotalCount()).isEqualTo(5);
            assertThat(response.getCurrentPage()).isEqualTo(0);
        }

        @Test
        @DisplayName("실패: 시작일이 종료일보다 늦으면 예외가 발생한다")
        void 실패_날짜범위오류() {
            // given
            TransactionHistoryRequest request = requestOf("2024-01-31", "2024-01-01");

            // when & then
            assertThatThrownBy(() -> accountService.getTransactionHistoryList(request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining(ErrorCode.INQUIRY_INVALID_DATE_RANGE.getMessage());
        }

        @Test
        @DisplayName("실패: 존재하지 않는 계좌 번호일 경우 예외가 발생한다")
        void 실패_계좌미존재() {
            // given
            mockDecrypt("invalid-acc", "900101");
            given(accountMapper.findByAccountNoPlain("invalid-acc")).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> accountService.getTransactionHistoryList(
                    requestOf("2024-01-01", "2024-01-31")))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining(ErrorCode.INQUIRY_ACCOUNT_NOTFOUND.getMessage());
        }

        @Test
        @DisplayName("실패: 소유주 정보(주민번호 앞자리)가 일치하지 않으면 예외가 발생한다")
        void 실패_소유주불일치() {
            // given
            mockDecrypt("acc-123", "800101");

            Account account = Account.builder()
                    .id(1L)
                    .customerId(10L)
                    .build();

            Customer customer = Customer.builder()
                    .id(10L)
                    .rrnPrefix("900101") // 다름
                    .build();

            given(accountMapper.findByAccountNoPlain("acc-123")).willReturn(Optional.of(account));
            given(customerMapper.findById(10L)).willReturn(Optional.of(customer));

            // when & then
            assertThatThrownBy(() -> accountService.getTransactionHistoryList(
                    requestOf("2024-01-01", "2024-01-31")))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining(ErrorCode.USER_NOT_FOUND.getMessage());
        }
    }
}
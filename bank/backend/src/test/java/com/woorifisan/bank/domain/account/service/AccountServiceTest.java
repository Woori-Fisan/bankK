package com.woorifisan.bank.domain.account.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

import com.woorifisan.bank.domain.account.dto.request.TransactionHistoryRequest;
import com.woorifisan.bank.domain.account.dto.response.TransactionHistoryResponse;
import com.woorifisan.bank.domain.account.mapper.AccountMapper;
import com.woorifisan.bank.domain.account.mapper.TransactionLedgerMapper;
import com.woorifisan.bank.domain.account.model.Account;
import com.woorifisan.bank.domain.customer.mapper.CustomerMapper;
import com.woorifisan.bank.domain.customer.model.Customer;
import com.woorifisan.bank.global.exception.BusinessException;
import com.woorifisan.bank.global.response.ErrorCode;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
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

    @Nested
    @DisplayName("거래 내역 조회 테스트")
    class GetTransactionHistoryList {

        @Test
        @DisplayName("성공: 유효한 계좌와 본인 정보로 거래 내역을 조회할 수 있다")
        void 성공_거래내역조회() {
            // given
            TransactionHistoryRequest request = TransactionHistoryRequest.builder()
                    .accountNo("hash_123")
                    .customerRrnPrefix("900101")
                    .startDate("2024-01-01")
                    .endDate("2024-01-31")
                    .page(0)
                    .size(10)
                    .build();

            Account account = Account.builder()
                    .id(1L)
                    .customerId(10L)
                    .accountNoHash("hash_123")
                    .build();

            Customer customer = Customer.builder()
                    .id(10L)
                    .rrnPrefixEnc("900101")
                    .build();

            given(accountMapper.findByAccountNoHash("hash_123")).willReturn(Optional.of(account));
            given(customerMapper.findById(10L)).willReturn(Optional.of(customer));
            given(transactionLedgerMapper.countHistory(anyLong(), anyString(), anyString())).willReturn(5);
            given(transactionLedgerMapper.findHistoryList(anyLong(), anyString(), anyString(), anyInt(), anyInt()))
                    .willReturn(List.of());

            // when
            TransactionHistoryResponse response = accountService.getTransactionHistoryList(request);

            // then
            assertThat(response.getTotalCount()).isEqualTo(5);
            assertThat(response.getCurrentPage()).isEqualTo(0);
        }

        @Test
        @DisplayName("실패: 시작일이 종료일보다 늦으면 예외가 발생한다")
        void 실패_날짜범위오류() {
            // given
            TransactionHistoryRequest request = TransactionHistoryRequest.builder()
                    .startDate("2024-01-31")
                    .endDate("2024-01-01")
                    .build();

            // when & then
            assertThatThrownBy(() -> accountService.getTransactionHistoryList(request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining(ErrorCode.INQUIRY_INVALID_DATE_RANGE.getMessage());
        }

        @Test
        @DisplayName("실패: 존재하지 않는 계좌 해시일 경우 예외가 발생한다")
        void 실패_계좌미존재() {
            // given
            TransactionHistoryRequest request = TransactionHistoryRequest.builder()
                    .accountNo("invalid_hash")
                    .startDate("2024-01-01")
                    .endDate("2024-01-31")
                    .build();

            given(accountMapper.findByAccountNoHash("invalid_hash")).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> accountService.getTransactionHistoryList(request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining(ErrorCode.INQUIRY_ACCOUNT_NOTFOUND.getMessage());
        }

        @Test
        @DisplayName("실패: 소유주 정보(주민번호 앞자리)가 일치하지 않으면 예외가 발생한다")
        void 실패_소유주불일치() {
            // given
            TransactionHistoryRequest request = TransactionHistoryRequest.builder()
                    .accountNo("hash_123")
                    .customerRrnPrefix("800101")
                    .startDate("2024-01-01")
                    .endDate("2024-01-31")
                    .build();

            Account account = Account.builder()
                    .id(1L)
                    .customerId(10L)
                    .build();

            Customer customer = Customer.builder()
                    .id(10L)
                    .rrnPrefixEnc("900101") // 다름
                    .build();

            given(accountMapper.findByAccountNoHash("hash_123")).willReturn(Optional.of(account));
            given(customerMapper.findById(10L)).willReturn(Optional.of(customer));

            // when & then
            assertThatThrownBy(() -> accountService.getTransactionHistoryList(request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining(ErrorCode.USER_NOT_FOUND.getMessage());
        }
    }

    private int anyInt() {
        return org.mockito.ArgumentMatchers.anyInt();
    }
}

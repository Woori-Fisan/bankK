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
import static org.mockito.BDDMockito.willThrow;

import com.woorifisan.bank.domain.account.dto.decrypted.DecryptedInquiryData;
import com.woorifisan.bank.domain.account.dto.request.BalanceInquiryRequest;
import com.woorifisan.bank.domain.account.dto.request.TransactionHistoryRequest;
import com.woorifisan.bank.domain.account.dto.response.BalanceInquiryResponse;
import com.woorifisan.bank.domain.account.dto.response.TransactionHistoryResponse;
import com.woorifisan.bank.domain.account.mapper.AccountMapper;
import com.woorifisan.bank.domain.account.mapper.TransactionLedgerMapper;
import com.woorifisan.bank.domain.account.model.Account;
import com.woorifisan.bank.domain.customer.service.CustomerService;
import com.woorifisan.bank.global.exception.BusinessException;
import com.woorifisan.bank.global.response.ErrorCode;
import com.woorifisan.bank.global.security.service.SecurityService;

import java.math.BigDecimal;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;
import javax.crypto.SecretKey;
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
    private CustomerService customerService;

    @Mock
    private SecurityService securityService;

    /**
     * 복호화 성공 결과를 강제로 반환하는 Helper 메서드 (이름 파라미터 추가)
     */
    private void mockDecrypt(String accountNo, String rrnPrefix, String customerName) {
        DecryptedInquiryData data = new DecryptedInquiryData();
        data.setAccountNo(accountNo);
        data.setCustomerRrnPrefix(rrnPrefix);
        data.setCustomerName(customerName);
        SecurityService.DecryptionResult<DecryptedInquiryData> result =
                new SecurityService.DecryptionResult<>(data, mock(SecretKey.class));
        given(securityService.decryptWithKey(any(), eq(DecryptedInquiryData.class)))
                .willReturn(result);
    }

    /**
     * 거래내역 요청 DTO 조립을 위한 Helper 메서드 (SuperBuilder 적용)
     */
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
    @DisplayName("잔액 조회 테스트")
    class GetBalance {

        @Test
        @DisplayName("유효한 계좌와 본인 정보로 잔액을 조회할 수 있다")
        void 성공_잔액조회() {
            // given
            String accountNo = "acc-123";
            mockDecrypt(accountNo, "900101", "홍길동");
            given(securityService.encryptResponse(any(), any(SecretKey.class)))
                    .willReturn("encrypted-payload");

            Account account = Account.builder()
                    .id(1L)
                    .customerId(10L)
                    .accountNo(accountNo)
                    .balance(BigDecimal.valueOf(50000L))
                    .status("NORMAL")
                    .build();

            given(accountMapper.findByAccountNoPlain(accountNo)).willReturn(Optional.of(account));

            BalanceInquiryRequest request = BalanceInquiryRequest.builder()
                    .reqPayload("dummy-jwe")
                    .bankKeyId("test-key")
                    .build();

            // when
            BalanceInquiryResponse response = accountService.getBalance(request);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getResPayload()).isEqualTo("encrypted-payload");
            assertThat(response.getStatus()).isEqualTo("NORMAL");
        }

        @Test
        @DisplayName("존재하지 않는 계좌 번호일 경우 예외가 발생한다")
        void 실패_계좌미존재() {
            // given
            String accountNo = "invalid-acc";
            mockDecrypt(accountNo, "900101", "홍길동");
            given(accountMapper.findByAccountNoPlain(accountNo)).willReturn(Optional.empty());

            BalanceInquiryRequest request = BalanceInquiryRequest.builder()
                    .reqPayload("dummy-jwe")
                    .bankKeyId("test-key")
                    .build();

            // when & then
            assertThatThrownBy(() -> accountService.getBalance(request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining(ErrorCode.ACCOUNT_NOT_FOUND.getMessage());
        }

        @Test
        @DisplayName("소유주 주민번호 앞자리가 일치하지 않으면 예외가 발생한다")
        void 실패_주민번호앞자리불일치() {
            // given
            String accountNo = "acc-123";
            mockDecrypt(accountNo, "800101", "홍길동");

            Account account = Account.builder()
                    .id(1L)
                    .customerId(10L)
                    .build();

            given(accountMapper.findByAccountNoPlain(accountNo)).willReturn(Optional.of(account));
            willThrow(new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND))
                    .given(customerService).verifyCustomerIdentification(anyLong(), eq("800101"), eq("홍길동"));

            BalanceInquiryRequest request = BalanceInquiryRequest.builder()
                    .reqPayload("dummy-jwe")
                    .bankKeyId("test-key")
                    .build();

            // when & then
            assertThatThrownBy(() -> accountService.getBalance(request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining(ErrorCode.ACCOUNT_NOT_FOUND.getMessage());
        }

        @Test
        @DisplayName("소유주 이름이 일치하지 않으면 예외가 발생한다")
        void 실패_고객이름불일치() {
            // given
            String accountNo = "acc-123";
            mockDecrypt(accountNo, "900101", "이순신"); // 복호화 정보와 불일치하는 이름 입력

            Account account = Account.builder()
                    .id(1L)
                    .customerId(10L)
                    .build();

            given(accountMapper.findByAccountNoPlain(accountNo)).willReturn(Optional.of(account));
            willThrow(new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND))
                    .given(customerService).verifyCustomerIdentification(anyLong(), eq("900101"), eq("이순신"));

            BalanceInquiryRequest request = BalanceInquiryRequest.builder()
                    .reqPayload("dummy-jwe")
                    .bankKeyId("test-key")
                    .build();

            // when & then
            assertThatThrownBy(() -> accountService.getBalance(request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining(ErrorCode.ACCOUNT_NOT_FOUND.getMessage());
        }
    }

    @Nested
    @DisplayName("거래 내역 조회 테스트")
    class GetTransactionHistoryList {

        @Test
        @DisplayName("유효한 계좌와 본인 정보로 거래 내역을 조회할 수 있다")
        void 성공_거래내역조회() {
            // given
            mockDecrypt("acc-123", "900101", "홍길동");
            given(securityService.encryptResponse(any(), any(SecretKey.class)))
                    .willReturn("encrypted-payload");

            Account account = Account.builder()
                    .id(1L)
                    .customerId(10L)
                    .accountNo("acc-123")
                    .build();

            given(accountMapper.findByAccountNoPlain("acc-123")).willReturn(Optional.of(account));
            given(transactionLedgerMapper.countHistory(anyLong(), anyString(), anyString())).willReturn(5);
            given(transactionLedgerMapper.findHistoryList(anyLong(), anyString(), anyString(), anyInt(), anyInt()))
                    .willReturn(List.of());

            // when
            TransactionHistoryResponse response = accountService.getTransactionHistoryList(
                    requestOf("2024-01-01", "2024-01-31"));

            // then
            assertThat(response).isNotNull();
            assertThat(response.getTotalCount()).isEqualTo(5);
            assertThat(response.getCurrentPage()).isEqualTo(0);
        }

        @Test
        @DisplayName("시작일과 종료일이 같을 때 정상 조회가 가능하다")
        void 성공_동일날짜_조회() {
            // given
            mockDecrypt("acc-123", "900101", "홍길동");
            given(securityService.encryptResponse(any(), any(SecretKey.class)))
                    .willReturn("encrypted-payload");

            Account account = Account.builder()
                    .id(1L)
                    .customerId(10L)
                    .accountNo("acc-123")
                    .build();

            given(accountMapper.findByAccountNoPlain("acc-123")).willReturn(Optional.of(account));
            given(transactionLedgerMapper.countHistory(anyLong(), anyString(), anyString())).willReturn(0);
            given(transactionLedgerMapper.findHistoryList(anyLong(), anyString(), anyString(), anyInt(), anyInt()))
                    .willReturn(List.of());

            // when
            TransactionHistoryResponse response = accountService.getTransactionHistoryList(
                    requestOf("2024-01-01", "2024-01-01"));

            // then
            assertThat(response).isNotNull();
            assertThat(response.getTotalCount()).isEqualTo(0);
        }

        @Test
        @DisplayName("시작일이 종료일보다 늦으면 예외가 발생한다")
        void 실패_날짜범위오류() {
            // given
            TransactionHistoryRequest request = requestOf("2024-01-31", "2024-01-01");

            // when & then
            assertThatThrownBy(() -> accountService.getTransactionHistoryList(request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining(ErrorCode.INQUIRY_INVALID_DATE_RANGE.getMessage());
        }

        @Test
        @DisplayName("시작일의 날짜 포맷이 올바르지 않으면 예외가 발생한다")
        void 실패_시작일날짜포맷오류() {
            // given
            TransactionHistoryRequest request = requestOf("20240101", "2024-01-31");

            // when & then
            assertThatThrownBy(() -> accountService.getTransactionHistoryList(request))
                    .isInstanceOf(DateTimeParseException.class);
        }

        @Test
        @DisplayName("종료일의 날짜 포맷이 올바르지 않으면 예외가 발생한다")
        void 실패_종료일날짜포맷오류() {
            // given
            TransactionHistoryRequest request = requestOf("2024-01-01", "20240131");

            // when & then
            assertThatThrownBy(() -> accountService.getTransactionHistoryList(request))
                    .isInstanceOf(DateTimeParseException.class);
        }

        @Test
        @DisplayName("시작일 날짜에 시분초가 유입되면 예외가 발생한다")
        void 실패_시작일시분초유입오류() {
            // given
            TransactionHistoryRequest request = requestOf("2024-01-01 10:00:00", "2024-01-31");

            // when & then
            assertThatThrownBy(() -> accountService.getTransactionHistoryList(request))
                    .isInstanceOf(DateTimeParseException.class);
        }

        @Test
        @DisplayName("종료일 날짜에 시분초가 유입되면 예외가 발생한다")
        void 실패_종료일시분초유입오류() {
            // given
            TransactionHistoryRequest request = requestOf("2024-01-01", "2024-01-31 12:00:00");

            // when & then
            assertThatThrownBy(() -> accountService.getTransactionHistoryList(request))
                    .isInstanceOf(DateTimeParseException.class);
        }

        @Test
        @DisplayName("존재하지 않는 계좌 번호일 경우 예외가 발생한다")
        void 실패_계좌미존재() {
            // given
            mockDecrypt("invalid-acc", "900101", "홍길동");
            given(accountMapper.findByAccountNoPlain("invalid-acc")).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> accountService.getTransactionHistoryList(
                    requestOf("2024-01-01", "2024-01-31")))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining(ErrorCode.INQUIRY_ACCOUNT_NOTFOUND.getMessage());
        }

        @Test
        @DisplayName("소유주 주민번호 앞자리가 일치하지 않으면 예외가 발생한다")
        void 실패_주민번호앞자리불일치() {
            // given
            mockDecrypt("acc-123", "800101", "홍길동");

            Account account = Account.builder()
                    .id(1L)
                    .customerId(10L)
                    .build();

            given(accountMapper.findByAccountNoPlain("acc-123")).willReturn(Optional.of(account));
            willThrow(new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND))
                    .given(customerService).verifyCustomerIdentification(anyLong(), eq("800101"), eq("홍길동"));

            // when & then
            assertThatThrownBy(() -> accountService.getTransactionHistoryList(
                    requestOf("2024-01-01", "2024-01-31")))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining(ErrorCode.ACCOUNT_NOT_FOUND.getMessage());
        }

        @Test
        @DisplayName("소유주 이름이 일치하지 않으면 예외가 발생한다")
        void 실패_고객이름불일치() {
            // given
            mockDecrypt("acc-123", "900101", "이순신");

            Account account = Account.builder()
                    .id(1L)
                    .customerId(10L)
                    .build();

            given(accountMapper.findByAccountNoPlain("acc-123")).willReturn(Optional.of(account));
            willThrow(new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND))
                    .given(customerService).verifyCustomerIdentification(anyLong(), eq("900101"), eq("이순신"));

            // when & then
            assertThatThrownBy(() -> accountService.getTransactionHistoryList(
                    requestOf("2024-01-01", "2024-01-31")))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining(ErrorCode.ACCOUNT_NOT_FOUND.getMessage());
        }
    }
}
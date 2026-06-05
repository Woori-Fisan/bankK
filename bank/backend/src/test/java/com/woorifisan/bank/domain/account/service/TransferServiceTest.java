package com.woorifisan.bank.domain.account.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.ArgumentMatchers.anyString;

import com.woorifisan.bank.domain.account.dto.decrypted.DecryptedWithdrawData;
import com.woorifisan.bank.domain.account.dto.request.TransferRequest;
import com.woorifisan.bank.domain.account.dto.response.TransferResponse;
import com.woorifisan.bank.domain.account.mapper.AccountMapper;
import com.woorifisan.bank.domain.account.mapper.TransactionLedgerMapper;
import com.woorifisan.bank.domain.account.model.Account;
import com.woorifisan.bank.domain.customer.mapper.CustomerMapper;
import com.woorifisan.bank.domain.customer.model.Customer;
import com.woorifisan.bank.global.security.service.SecurityService;
import java.math.BigDecimal;
import java.util.Optional;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.http.HttpStatus;
import com.woorifisan.bank.global.config.BankNetworkConfig;
import java.util.Map;
import java.util.HashMap;

@ExtendWith(MockitoExtension.class)
class TransferServiceTest {

    @InjectMocks
    private TransferService transferService;

    @Mock
    private AccountMapper accountMapper;

    @Mock
    private CustomerMapper customerMapper;

    @Mock
    private TransactionLedgerMapper transactionLedgerMapper;

    @Mock
    private SecurityService securityService;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private BankNetworkConfig bankNetworkConfig;

    @Mock
    private TransferTxService transferTxService; // 신규 서브 트랜잭션 서비스 Mock 추가

    private Account sender;
    private Customer senderCustomer;

    private static final String OUR_BANK_CODE = "020";
    private static final String OTHER_BANK_CODE = "004";

    @BeforeEach
    void setUp() {
        sender = Account.builder()
                .id(1L)
                .customerId(10L)
                .accountNo("111-111")
                .balance(new BigDecimal("100000"))
                .password("hashedPassword")
                .status("NORMAL")
                .version(1)
                .build();

        senderCustomer = Customer.builder()
                .id(10L)
                .customerName("김철수")
                .rrnPrefix("9001011")
                .build();
    }

    @Test
    @DisplayName("위임: 출금 이체 호출 시 TransferTxService에 위임한다")
    void withdrawTransfer_delegation() {
        // given
        TransferRequest request = TransferRequest.builder().build();
        TransferResponse mockResponse = TransferResponse.builder().build();
        given(transferTxService.withdrawTransfer(request)).willReturn(mockResponse);

        // when
        TransferResponse response = transferService.withdrawTransfer(request);

        // then
        verify(transferTxService).withdrawTransfer(request);
        assertEquals(mockResponse, response);
    }

    @Test
    @DisplayName("위임: 이체 환불 호출 시 TransferTxService에 위임한다")
    void refundTransfer_delegation() {
        // given
        TransferRequest request = TransferRequest.builder().build();
        TransferResponse mockResponse = TransferResponse.builder().build();
        given(transferTxService.refundTransfer(request)).willReturn(mockResponse);

        // when
        TransferResponse response = transferService.refundTransfer(request);

        // then
        verify(transferTxService).refundTransfer(request);
        assertEquals(mockResponse, response);
    }

    @Test
    @DisplayName("이중지급방지: 타행 이체 중 API 에러가 났으나 상대 은행 상태 조회 시 성공으로 확인되면 성공 처리하고 환불을 방지한다")
    void executeTransfer_doublePaymentPrevention_success() {
        // given
        TransferRequest request = TransferRequest.builder()
                .withdrawalBankCode(OUR_BANK_CODE)
                .depositBankCode(OTHER_BANK_CODE) // 타행 이체
                .amount(new BigDecimal("10000"))
                .reqPayload("encrypted-jwe")
                .bankKeyId("key-id")
                .build();

        DecryptedWithdrawData decryptedData = DecryptedWithdrawData.builder()
                .withdrawalAccountNo("111-111")
                .withdrawalPassword("1234")
                .customerRrnPrefix("9001011")
                .depositAccountNo("999-999")
                .build();

        // 1. 공통 복호화 Mock
        given(securityService.decryptWithKey(any(), eq(DecryptedWithdrawData.class)))
                .willReturn(new SecurityService.DecryptionResult<>(decryptedData, new SecretKeySpec(new byte[16], "AES")));
        
        // 2. TransferTxService 출금 Mocking
        TransferResponse mockWithdrawalResponse = TransferResponse.of("mock-tx-id", "2026-06-05 17:00:00", new BigDecimal("90000"));
        given(transferTxService.withdrawTransfer(request)).willReturn(mockWithdrawalResponse);

        // 3. 타행 정보 및 API URL 설정 Mock
        BankNetworkConfig.BankProperty bankProperty = new BankNetworkConfig.BankProperty();
        bankProperty.setBaseUrl("http://mock-bank");
        given(bankNetworkConfig.getBankProperty(OTHER_BANK_CODE)).willReturn(bankProperty);

        // 4. 타행 입금 API 호출 시 ResourceAccessException (타임아웃 등 통신에러) 강제 발생
        given(restTemplate.postForEntity(anyString(), any(), eq(Object.class)))
                .willThrow(new ResourceAccessException("Read timed out"));

        // 5. 타행 거래 상태 조회 API 호출 Mock (1회차는 NotFound 예외, 2회차는 SUCCESS 반환)
        Map<String, Object> mockSuccessResponse = new HashMap<>();
        Map<String, Object> mockData = new HashMap<>();
        mockData.put("status", "SUCCESS");
        mockSuccessResponse.put("data", mockData);

        given(restTemplate.getForObject(anyString(), eq(Map.class)))
                .willThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND)) // 1회차 조회 실패
                .willReturn(mockSuccessResponse);                             // 2회차 조회 성공

        // when
        TransferResponse response = transferService.executeTransfer(request);

        // then
        assertNotNull(response);
        assertEquals("mock-tx-id", response.getTransactionId());
        
        // 출금 원장은 처음 PENDING으로 저장(withdrawTransfer 내부)되었다가, 최종 SUCCESS로 성공 마킹이 되어야 함
        verify(transferTxService).updateLedgerStatus("mock-tx-id", "SUCCESS");
        
        // 이중 지급을 차단하기 위해 환불이 호출되지 않았음을 검증
        verify(transferTxService, never()).refundTransfer(any(), anyString());
    }

    @Test
    @DisplayName("이중지급방지: 타행 이체 중 API 에러가 나고 상태 조회 API마저 최종 실패(UNKNOWN)하면 예외를 던져 환불을 차단하고 PENDING 상태를 유지한다")
    void executeTransfer_doublePaymentPrevention_unknown() {
        // given
        TransferRequest request = TransferRequest.builder()
                .withdrawalBankCode(OUR_BANK_CODE)
                .depositBankCode(OTHER_BANK_CODE)
                .amount(new BigDecimal("10000"))
                .reqPayload("encrypted-jwe")
                .bankKeyId("key-id")
                .build();

        DecryptedWithdrawData decryptedData = DecryptedWithdrawData.builder()
                .withdrawalAccountNo("111-111")
                .withdrawalPassword("1234")
                .customerRrnPrefix("9001011")
                .depositAccountNo("999-999")
                .build();

        // 1. 공통 복호화 Mock
        given(securityService.decryptWithKey(any(), eq(DecryptedWithdrawData.class)))
                .willReturn(new SecurityService.DecryptionResult<>(decryptedData, new SecretKeySpec(new byte[16], "AES")));
        
        // 2. TransferTxService 출금 Mocking
        TransferResponse mockWithdrawalResponse = TransferResponse.of("mock-tx-id", "2026-06-05 17:00:00", new BigDecimal("90000"));
        given(transferTxService.withdrawTransfer(request)).willReturn(mockWithdrawalResponse);

        // 3. 타행 정보 및 API URL 설정 Mock
        BankNetworkConfig.BankProperty bankProperty = new BankNetworkConfig.BankProperty();
        bankProperty.setBaseUrl("http://mock-bank");
        given(bankNetworkConfig.getBankProperty(OTHER_BANK_CODE)).willReturn(bankProperty);

        // 4. 타행 입금 API 호출 시 ResourceAccessException 강제 발생
        given(restTemplate.postForEntity(anyString(), any(), eq(Object.class)))
                .willThrow(new ResourceAccessException("Read timed out"));

        // 5. 타행 거래 상태 조회 API 호출 Mock (10회 시도 모두 NotFound 예외 발생 유도)
        given(restTemplate.getForObject(anyString(), eq(Map.class)))
                .willThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND));

        // when & then
        org.junit.jupiter.api.Assertions.assertThrows(com.woorifisan.bank.global.exception.BusinessException.class, () -> {
            transferService.executeTransfer(request);
        });

        // 상태를 알 수 없으므로 성공 마킹(SUCCESS)이나 실패 마킹(FAILED)이 진행되지 않고 PENDING으로 남아있어야 함
        verify(transferTxService, never()).updateLedgerStatus(anyString(), anyString());
        verify(transferTxService, never()).refundTransfer(any(), anyString());
    }

    @Test
    @DisplayName("이중지급방지: 타행 이체 중 API 에러가 나고 상태 조회 시 해결 불가능한 400 에러 등이 발생하면 즉시 예외를 던져 재시도를 중단(Fail-Fast)한다")
    void executeTransfer_doublePaymentPrevention_failFast() {
        // given
        TransferRequest request = TransferRequest.builder()
                .withdrawalBankCode(OUR_BANK_CODE)
                .depositBankCode(OTHER_BANK_CODE)
                .amount(new BigDecimal("10000"))
                .reqPayload("encrypted-jwe")
                .bankKeyId("key-id")
                .build();

        DecryptedWithdrawData decryptedData = DecryptedWithdrawData.builder()
                .withdrawalAccountNo("111-111")
                .withdrawalPassword("1234")
                .customerRrnPrefix("9001011")
                .depositAccountNo("999-999")
                .build();

        // 1. 공통 복호화 Mock
        given(securityService.decryptWithKey(any(), eq(DecryptedWithdrawData.class)))
                .willReturn(new SecurityService.DecryptionResult<>(decryptedData, new SecretKeySpec(new byte[16], "AES")));
        
        // 2. TransferTxService 출금 Mocking
        TransferResponse mockWithdrawalResponse = TransferResponse.of("mock-tx-id", "2026-06-05 17:00:00", new BigDecimal("90000"));
        given(transferTxService.withdrawTransfer(request)).willReturn(mockWithdrawalResponse);

        // 3. 타행 정보 및 API URL 설정 Mock
        BankNetworkConfig.BankProperty bankProperty = new BankNetworkConfig.BankProperty();
        bankProperty.setBaseUrl("http://mock-bank");
        given(bankNetworkConfig.getBankProperty(OTHER_BANK_CODE)).willReturn(bankProperty);

        // 4. 타행 입금 API 호출 시 ResourceAccessException 강제 발생
        given(restTemplate.postForEntity(anyString(), any(), eq(Object.class)))
                .willThrow(new ResourceAccessException("Read timed out"));

        // 5. 타행 거래 상태 조회 API 호출 Mock (1회차에 바로 400 BadRequest 발생시킴)
        given(restTemplate.getForObject(anyString(), eq(Map.class)))
                .willThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST));

        // when & then
        org.junit.jupiter.api.Assertions.assertThrows(com.woorifisan.bank.global.exception.BusinessException.class, () -> {
            transferService.executeTransfer(request);
        });

        // 400 에러 시 즉시 예외를 던져 루프를 탈출했으므로, 상태 조회 API 호출 횟수가 단 1회여야 함
        verify(restTemplate, times(1)).getForObject(anyString(), eq(Map.class));

        // 상태를 알 수 없으므로 성공 마킹(SUCCESS)이나 실패 마킹(FAILED)이 진행되지 않고 PENDING으로 남아있어야 함
        verify(transferTxService, never()).updateLedgerStatus(anyString(), anyString());
        verify(transferTxService, never()).refundTransfer(any(), anyString());
    }

    @Test
    @DisplayName("이중지급방지: 타행 이체 중 API 에러가 나고 상태 조회 시 예기치 않은 일반 예외가 발생하면 즉시 예외를 던져 재시도를 중단(Fail-Fast)한다")
    void executeTransfer_doublePaymentPrevention_generalExceptionFailFast() {
        // given
        TransferRequest request = TransferRequest.builder()
                .withdrawalBankCode(OUR_BANK_CODE)
                .depositBankCode(OTHER_BANK_CODE)
                .amount(new BigDecimal("10000"))
                .reqPayload("encrypted-jwe")
                .bankKeyId("key-id")
                .build();

        DecryptedWithdrawData decryptedData = DecryptedWithdrawData.builder()
                .withdrawalAccountNo("111-111")
                .withdrawalPassword("1234")
                .customerRrnPrefix("9001011")
                .depositAccountNo("999-999")
                .build();

        // 1. 공통 복호화 Mock
        given(securityService.decryptWithKey(any(), eq(DecryptedWithdrawData.class)))
                .willReturn(new SecurityService.DecryptionResult<>(decryptedData, new SecretKeySpec(new byte[16], "AES")));
        
        // 2. TransferTxService 출금 Mocking
        TransferResponse mockWithdrawalResponse = TransferResponse.of("mock-tx-id", "2026-06-05 17:00:00", new BigDecimal("90000"));
        given(transferTxService.withdrawTransfer(request)).willReturn(mockWithdrawalResponse);

        // 3. 타행 정보 및 API URL 설정 Mock
        BankNetworkConfig.BankProperty bankProperty = new BankNetworkConfig.BankProperty();
        bankProperty.setBaseUrl("http://mock-bank");
        given(bankNetworkConfig.getBankProperty(OTHER_BANK_CODE)).willReturn(bankProperty);

        // 4. 타행 입금 API 호출 시 ResourceAccessException 강제 발생
        given(restTemplate.postForEntity(anyString(), any(), eq(Object.class)))
                .willThrow(new ResourceAccessException("Read timed out"));

        // 5. 타행 거래 상태 조회 API 호출 Mock (1회차에 바로 런타임 예외 발생시킴)
        given(restTemplate.getForObject(anyString(), eq(Map.class)))
                .willThrow(new RuntimeException("Unexpected DB Connection Failure"));

        // when & then
        org.junit.jupiter.api.Assertions.assertThrows(com.woorifisan.bank.global.exception.BusinessException.class, () -> {
            transferService.executeTransfer(request);
        });

        // 일반 예외 발생 시 즉시 예외를 던져 루프를 탈출했으므로, 상태 조회 API 호출 횟수가 단 1회여야 함
        verify(restTemplate, times(1)).getForObject(anyString(), eq(Map.class));

        // 상태를 알 수 없으므로 성공 마킹(SUCCESS)이나 실패 마킹(FAILED)이 진행되지 않고 PENDING으로 남아있어야 함
        verify(transferTxService, never()).updateLedgerStatus(anyString(), anyString());
        verify(transferTxService, never()).refundTransfer(any(), anyString());
    }
}

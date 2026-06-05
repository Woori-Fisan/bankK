package com.woorifisan.bank.domain.account.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.http.HttpStatus;
import com.woorifisan.bank.global.config.BankNetworkConfig;
import java.util.Map;
import java.util.HashMap;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.ArgumentMatchers.anyString;

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
    private PasswordEncoder passwordEncoder;

    @Mock
    private SecurityService securityService;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private BankNetworkConfig bankNetworkConfig;

    private Account sender;
    private Account receiver;
    private Customer senderCustomer;

    private static final String OUR_BANK_CODE = "020";
    private static final String OTHER_BANK_CODE = "004";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(transferService, "self", transferService);
        sender = Account.builder()
                .id(1L)
                .customerId(10L)
                .accountNo("111-111")
                .balance(new BigDecimal("100000"))
                .password("hashedPassword")
                .status("NORMAL")
                .version(1)
                .build();

        receiver = Account.builder()
                .id(2L)
                .customerId(20L)
                .accountNo("222-222")
                .balance(new BigDecimal("50000"))
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
    @DisplayName("출금 이체(타행출금 포함)가 성공한다")
    void withdrawTransfer_success() {
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

        given(securityService.decryptWithKey(any(), eq(DecryptedWithdrawData.class)))
                .willReturn(new SecurityService.DecryptionResult<>(decryptedData, new SecretKeySpec(new byte[16], "AES")));
        
        given(accountMapper.findByAccountNoPlain("111-111")).willReturn(Optional.of(sender));
        given(customerMapper.findById(10L)).willReturn(Optional.of(senderCustomer));
        given(passwordEncoder.matches("1234", "hashedPassword")).willReturn(true);
        given(accountMapper.findByIdForUpdate(1L)).willReturn(Optional.of(sender));
        given(accountMapper.updateBalance(sender.getId(), new BigDecimal("10000").negate(), sender.getVersion())).willReturn(1);
        given(securityService.encryptResponse(any(), any())).willReturn("encrypted-res");

        // when
        TransferResponse response = transferService.withdrawTransfer(request);

        // then
        assertNotNull(response);
        assertEquals(new BigDecimal("90000"), response.getBalanceAfter());
        assertEquals("encrypted-res", response.getResPayload());
        verify(accountMapper).updateBalance(sender.getId(), new BigDecimal("10000").negate(), sender.getVersion());
        verify(transactionLedgerMapper).insert(any());
    }



    @Test
    @DisplayName("이체 환불이 성공한다")
    void refundTransfer_success() {
        // given
        TransferRequest request = TransferRequest.builder()
                .withdrawalBankCode(OUR_BANK_CODE)
                .depositBankCode(OTHER_BANK_CODE)
                .amount(new BigDecimal("10000"))
                .reqPayload("original-withdraw-jwe")
                .bankKeyId("key-id")
                .build();

        DecryptedWithdrawData originalData = DecryptedWithdrawData.builder()
                .withdrawalAccountNo("111-111")
                .depositAccountNo("999-999")
                .build();

        given(securityService.decryptWithKey(any(), eq(DecryptedWithdrawData.class)))
                .willReturn(new SecurityService.DecryptionResult<>(originalData, new SecretKeySpec(new byte[16], "AES")));

        given(accountMapper.findByAccountNoPlain("111-111")).willReturn(Optional.of(sender));
        given(accountMapper.findByIdForUpdate(1L)).willReturn(Optional.of(sender));
        given(accountMapper.updateBalance(sender.getId(), new BigDecimal("10000"), sender.getVersion())).willReturn(1);

        // when
        TransferResponse response = transferService.refundTransfer(request);

        // then
        assertNotNull(response);
        assertEquals(new BigDecimal("110000"), response.getBalanceAfter());
        verify(accountMapper).updateBalance(sender.getId(), new BigDecimal("10000"), sender.getVersion());
        verify(transactionLedgerMapper).insert(any());
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
        
        // 2. 출금을 위한 계좌 및 고객 조회 Mock
        given(accountMapper.findByAccountNoPlain("111-111")).willReturn(Optional.of(sender));
        given(customerMapper.findById(10L)).willReturn(Optional.of(senderCustomer));
        given(passwordEncoder.matches("1234", "hashedPassword")).willReturn(true);
        given(accountMapper.findByIdForUpdate(1L)).willReturn(Optional.of(sender));
        given(accountMapper.updateBalance(sender.getId(), new BigDecimal("-10000"), sender.getVersion())).willReturn(1);
        given(securityService.encryptResponse(any(), any())).willReturn("encrypted-res");

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
        assertEquals(new BigDecimal("90000"), response.getBalanceAfter());
        
        // 출금 원장은 처음 PENDING으로 저장되었다가, 최종 SUCCESS로 성공 마킹이 되어야 함
        verify(transactionLedgerMapper).insert(any()); // 1회 insert (출금 시 PENDING 원장 기록)
        verify(transactionLedgerMapper).updateStatus(anyString(), eq("SUCCESS")); // 1회 update (상태 조회 성공 후 SUCCESS 마킹)
        
        // 이중 지급을 차단하기 위해 환불(deposit/insert) 호출이 단 한 번도 실행되지 않았음을 검증
        // 기존 원장 저장을 위해 insert가 이미 1회 호출되었으므로, insert가 추가(2회 이상) 호출되지 않았는지 검증
        verify(transactionLedgerMapper, times(1)).insert(any()); // 총 insert 횟수는 출금 시 1회여야 함
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
        
        // 2. 출금을 위한 계좌 및 고객 조회 Mock
        given(accountMapper.findByAccountNoPlain("111-111")).willReturn(Optional.of(sender));
        given(customerMapper.findById(10L)).willReturn(Optional.of(senderCustomer));
        given(passwordEncoder.matches("1234", "hashedPassword")).willReturn(true);
        given(accountMapper.findByIdForUpdate(1L)).willReturn(Optional.of(sender));
        given(accountMapper.updateBalance(sender.getId(), new BigDecimal("-10000"), sender.getVersion())).willReturn(1);
        given(securityService.encryptResponse(any(), any())).willReturn("encrypted-res");

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

        // 출금 원장은 처음 PENDING으로 저장(insert 1회)
        verify(transactionLedgerMapper, times(1)).insert(any());
        
        // 상태를 알 수 없으므로 성공 마킹(SUCCESS)이나 실패 마킹(FAILED)이 진행되지 않고 PENDING으로 남아있어야 함
        verify(transactionLedgerMapper, never()).updateStatus(anyString(), anyString());
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
        
        // 2. 출금을 위한 계좌 및 고객 조회 Mock
        given(accountMapper.findByAccountNoPlain("111-111")).willReturn(Optional.of(sender));
        given(customerMapper.findById(10L)).willReturn(Optional.of(senderCustomer));
        given(passwordEncoder.matches("1234", "hashedPassword")).willReturn(true);
        given(accountMapper.findByIdForUpdate(1L)).willReturn(Optional.of(sender));
        given(accountMapper.updateBalance(sender.getId(), new BigDecimal("-10000"), sender.getVersion())).willReturn(1);
        given(securityService.encryptResponse(any(), any())).willReturn("encrypted-res");

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

        // 출금 원장은 처음 PENDING으로 저장(insert 1회)
        verify(transactionLedgerMapper, times(1)).insert(any());
        
        // 상태를 알 수 없으므로 성공 마킹(SUCCESS)이나 실패 마킹(FAILED)이 진행되지 않고 PENDING으로 남아있어야 함
        verify(transactionLedgerMapper, never()).updateStatus(anyString(), anyString());
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
        
        // 2. 출금을 위한 계좌 및 고객 조회 Mock
        given(accountMapper.findByAccountNoPlain("111-111")).willReturn(Optional.of(sender));
        given(customerMapper.findById(10L)).willReturn(Optional.of(senderCustomer));
        given(passwordEncoder.matches("1234", "hashedPassword")).willReturn(true);
        given(accountMapper.findByIdForUpdate(1L)).willReturn(Optional.of(sender));
        given(accountMapper.updateBalance(sender.getId(), new BigDecimal("-10000"), sender.getVersion())).willReturn(1);
        given(securityService.encryptResponse(any(), any())).willReturn("encrypted-res");

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

        // 출금 원장은 처음 PENDING으로 저장(insert 1회)
        verify(transactionLedgerMapper, times(1)).insert(any());
        
        // 상태를 알 수 없으므로 성공 마킹(SUCCESS)이나 실패 마킹(FAILED)이 진행되지 않고 PENDING으로 남아있어야 함
        verify(transactionLedgerMapper, never()).updateStatus(anyString(), anyString());
    }
}


package com.woorifisan.bank.domain.account.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.woorifisan.bank.domain.account.dto.decrypted.DecryptedRecipientData;
import com.woorifisan.bank.domain.account.dto.decrypted.DecryptedWithdrawData;
import com.woorifisan.bank.domain.account.dto.request.InternalDepositRequest;
import com.woorifisan.bank.domain.account.dto.request.RecipientRequest;
import com.woorifisan.bank.domain.account.dto.request.TransferRequest;
import com.woorifisan.bank.domain.account.dto.response.RecipientResponse;
import com.woorifisan.bank.domain.account.dto.response.TransferResponse;
import com.woorifisan.bank.domain.account.dto.response.TransferStatusResponse;
import com.woorifisan.bank.domain.account.mapper.AccountMapper;
import com.woorifisan.bank.domain.account.mapper.TransactionLedgerMapper;
import com.woorifisan.bank.domain.account.model.Account;
import com.woorifisan.bank.domain.account.model.TransactionLedger;
import com.woorifisan.bank.domain.customer.mapper.CustomerMapper;
import com.woorifisan.bank.domain.customer.model.Customer;
import com.woorifisan.bank.global.config.BankNetworkConfig;
import com.woorifisan.bank.global.exception.BusinessException;
import com.woorifisan.bank.global.response.ErrorCode;
import com.woorifisan.bank.global.security.service.SecurityService;
import com.woorifisan.bank.global.util.CryptoUtil;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

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
    private TransferTxService transferTxService;

    @Mock
    private CryptoUtil cryptoUtil;

    private Account sender;
    private Customer senderCustomer;

    private static final String OUR_BANK_CODE = "020";
    private static final String OTHER_BANK_CODE = "004";
    private static final javax.crypto.SecretKey TEST_CEK = new SecretKeySpec(new byte[16], "AES");

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(transferService, "CURRENT_BANK_CODE", OUR_BANK_CODE);
        ReflectionTestUtils.setField(transferService, "CURRENT_BANK_NAME", "우리은행");

        sender = Account.builder()
                .id(1L)
                .customerId(10L)
                .accountNo("111-111")
                .balance(new BigDecimal("100000"))
                .password("hashedPassword")
                .accountType("DEPOSIT")
                .status("NORMAL")
                .build();

        senderCustomer = Customer.builder()
                .id(10L)
                .customerName("김철수")
                .rrnPrefix("9001011")
                .build();
    }

    @Test
    @DisplayName("수취인 확인이 성공한다")
    void verifyRecipient_success() {
        // given
        RecipientRequest request = RecipientRequest.builder()
                .depositBankCode(OUR_BANK_CODE)
                .bankKeyId("key-id")
                .build();

        DecryptedRecipientData decrypted = new DecryptedRecipientData();
        decrypted.setDepositAccountNo("111-111");

        given(securityService.decryptWithKey(eq(request), eq(DecryptedRecipientData.class)))
                .willReturn(new SecurityService.DecryptionResult<>(decrypted, TEST_CEK));
        given(cryptoUtil.hash("111-111")).willReturn("test-hash");
        given(accountMapper.findByAccountNoHash("test-hash")).willReturn(Optional.of(sender));
        given(customerMapper.findById(10L)).willReturn(Optional.of(senderCustomer));
        given(securityService.encryptResponse(any(), eq(TEST_CEK))).willReturn("encrypted-payload");

        // when
        RecipientResponse response = transferService.verifyRecipient(request);

        // then
        assertNotNull(response);
        assertEquals("encrypted-payload", response.getResPayload());
        assertEquals("우리은행", response.getDepositBankName());
        assertEquals("NORMAL", response.getAccountStatus());
    }

    @Test
    @DisplayName("수취인 확인 시 당행 은행코드가 아니면 예외가 발생한다")
    void verifyRecipient_invalidBankCode() {
        // given
        RecipientRequest request = RecipientRequest.builder()
                .depositBankCode(OTHER_BANK_CODE)
                .build();

        // when & then
        BusinessException ex = assertThrows(BusinessException.class, () ->
                transferService.verifyRecipient(request));
        assertEquals(ErrorCode.INVALID_INPUT, ex.getErrorCode());
    }

    @Test
    @DisplayName("수취인 확인 시 계좌를 찾을 수 없으면 예외가 발생한다")
    void verifyRecipient_accountNotFound() {
        // given
        RecipientRequest request = RecipientRequest.builder()
                .depositBankCode(OUR_BANK_CODE)
                .build();

        DecryptedRecipientData decrypted = new DecryptedRecipientData();
        decrypted.setDepositAccountNo("111-111");

        given(securityService.decryptWithKey(eq(request), eq(DecryptedRecipientData.class)))
                .willReturn(new SecurityService.DecryptionResult<>(decrypted, TEST_CEK));
        given(cryptoUtil.hash("111-111")).willReturn("test-hash");
        given(accountMapper.findByAccountNoHash("test-hash")).willReturn(Optional.empty());

        // when & then
        BusinessException ex = assertThrows(BusinessException.class, () ->
                transferService.verifyRecipient(request));
        assertEquals(ErrorCode.BANK_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("수취인 확인 시 고객 정보를 찾을 수 없으면 예외가 발생한다")
    void verifyRecipient_customerNotFound() {
        // given
        RecipientRequest request = RecipientRequest.builder()
                .depositBankCode(OUR_BANK_CODE)
                .build();

        DecryptedRecipientData decrypted = new DecryptedRecipientData();
        decrypted.setDepositAccountNo("111-111");

        given(securityService.decryptWithKey(eq(request), eq(DecryptedRecipientData.class)))
                .willReturn(new SecurityService.DecryptionResult<>(decrypted, TEST_CEK));
        given(cryptoUtil.hash("111-111")).willReturn("test-hash");
        given(accountMapper.findByAccountNoHash("test-hash")).willReturn(Optional.of(sender));
        given(customerMapper.findById(10L)).willReturn(Optional.empty());

        // when & then
        BusinessException ex = assertThrows(BusinessException.class, () ->
                transferService.verifyRecipient(request));
        assertEquals(ErrorCode.USER_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("위임: 출금 이체 호출 시 TransferTxService에 위임한다")
    void withdrawTransfer_delegation() {
        // given
        TransferRequest request = TransferRequest.builder().build();
        TransferResponse mockResponse = TransferResponse.builder().build();

        given(securityService.decryptWithKey(any(), eq(DecryptedWithdrawData.class)))
                .willReturn(new SecurityService.DecryptionResult<>(DecryptedWithdrawData.builder().build(), TEST_CEK));
        given(transferTxService.withdrawTransfer(eq(request), any(), any())).willReturn(mockResponse);

        // when
        TransferResponse response = transferService.withdrawTransfer(request);

        // then
        verify(transferTxService).withdrawTransfer(eq(request), any(), any());
        assertEquals(mockResponse, response);
    }

    @Test
    @DisplayName("위임: 이체 환불 호출 시 TransferTxService에 위임한다")
    void refundTransfer_delegation() {
        // given
        TransferRequest request = TransferRequest.builder().build();
        TransferResponse mockResponse = TransferResponse.builder().build();

        given(securityService.decryptWithKey(any(), eq(DecryptedWithdrawData.class)))
                .willReturn(new SecurityService.DecryptionResult<>(DecryptedWithdrawData.builder().build(), TEST_CEK));
        given(transferTxService.refundTransfer(eq(request), any())).willReturn(mockResponse);

        // when
        TransferResponse response = transferService.refundTransfer(request);

        // then
        verify(transferTxService).refundTransfer(eq(request), any());
        assertEquals(mockResponse, response);
    }

    @Test
    @DisplayName("위임: 원장 거래 ID를 동반한 이체 환불 호출 시 TransferTxService에 위임한다")
    void refundTransfer_withOriginalTxId_delegation() {
        // given
        TransferRequest request = TransferRequest.builder().build();
        TransferResponse mockResponse = TransferResponse.builder().build();
        String originalTxId = "orig-tx-id";

        given(securityService.decryptWithKey(any(), eq(DecryptedWithdrawData.class)))
                .willReturn(new SecurityService.DecryptionResult<>(DecryptedWithdrawData.builder().build(), TEST_CEK));
        given(transferTxService.refundTransfer(eq(request), any(), eq(originalTxId))).willReturn(mockResponse);

        // when
        TransferResponse response = transferService.refundTransfer(request, originalTxId);

        // then
        verify(transferTxService).refundTransfer(eq(request), any(), eq(originalTxId));
        assertEquals(mockResponse, response);
    }

    @Test
    @DisplayName("위임: 내부 입금 호출 시 TransferTxService에 위임한다")
    void internalDeposit_delegation() {
        // given
        InternalDepositRequest request = InternalDepositRequest.builder().build();
        TransferResponse mockResponse = TransferResponse.builder().build();

        given(transferTxService.internalDeposit(request)).willReturn(mockResponse);

        // when
        TransferResponse response = transferService.internalDeposit(request);

        // then
        verify(transferTxService).internalDeposit(request);
        assertEquals(mockResponse, response);
    }

    @Test
    @DisplayName("위임: 거래 상태 업데이트 호출 시 TransferTxService에 위임한다")
    void updateLedgerStatus_delegation() {
        // when
        transferService.updateLedgerStatus("tx-id", "SUCCESS");

        // then
        verify(transferTxService).updateLedgerStatus("tx-id", "SUCCESS");
    }

    @Test
    @DisplayName("이체 실행: 당행 이체 시 출금 성공하고 당행 입금도 성공한다")
    void executeTransfer_localTransfer_success() {
        // given
        TransferRequest request = TransferRequest.builder()
                .withdrawalBankCode(OUR_BANK_CODE)
                .depositBankCode(OUR_BANK_CODE)
                .amount(new BigDecimal("10000"))
                .build();

        DecryptedWithdrawData decryptedData = DecryptedWithdrawData.builder()
                .withdrawalAccountNo("111-111")
                .depositAccountNo("222-222")
                .build();

        given(securityService.decryptWithKey(eq(request), eq(DecryptedWithdrawData.class)))
                .willReturn(new SecurityService.DecryptionResult<>(decryptedData, TEST_CEK));

        TransferResponse mockWithdrawalResponse = TransferResponse.of("mock-tx-id", "2026-06-05 17:00:00", new BigDecimal("90000"));
        given(transferTxService.withdrawTransfer(eq(request), eq(decryptedData), eq(TEST_CEK))).willReturn(mockWithdrawalResponse);

        // when
        TransferResponse response = transferService.executeTransfer(request);

        // then
        assertNotNull(response);
        assertEquals("mock-tx-id", response.getTransactionId());
        verify(transferTxService).internalDeposit(any(InternalDepositRequest.class));
        verify(transferTxService).updateLedgerStatus("mock-tx-id", "SUCCESS");
    }

    @Test
    @DisplayName("이체 실행: 당행 이체 시 입금 실패하면 환불 처리를 수행하고 예외를 발생시킨다")
    void executeTransfer_localTransfer_depositFailure_refund() {
        // given
        TransferRequest request = TransferRequest.builder()
                .withdrawalBankCode(OUR_BANK_CODE)
                .depositBankCode(OUR_BANK_CODE)
                .amount(new BigDecimal("10000"))
                .build();

        DecryptedWithdrawData decryptedData = DecryptedWithdrawData.builder()
                .withdrawalAccountNo("111-111")
                .depositAccountNo("222-222")
                .build();

        given(securityService.decryptWithKey(eq(request), eq(DecryptedWithdrawData.class)))
                .willReturn(new SecurityService.DecryptionResult<>(decryptedData, TEST_CEK));

        TransferResponse mockWithdrawalResponse = TransferResponse.of("mock-tx-id", "2026-06-05 17:00:00", new BigDecimal("90000"));
        given(transferTxService.withdrawTransfer(eq(request), eq(decryptedData), eq(TEST_CEK))).willReturn(mockWithdrawalResponse);

        doThrow(new RuntimeException("DB error during deposit"))
                .when(transferTxService).internalDeposit(any(InternalDepositRequest.class));

        // when & then
        BusinessException ex = assertThrows(BusinessException.class, () ->
                transferService.executeTransfer(request));
        assertEquals(ErrorCode.LOAN_DEPOSIT_BANK_MISMATCH, ex.getErrorCode());
        verify(transferTxService).refundTransfer(eq(request), eq(decryptedData), eq("mock-tx-id"));
    }

    @Test
    @DisplayName("이체 실행: 타행 이체 시 직접 호출이 성공하면 원장 상태를 SUCCESS로 변경한다")
    void executeTransfer_externalTransfer_success() {
        // given
        TransferRequest request = TransferRequest.builder()
                .withdrawalBankCode(OUR_BANK_CODE)
                .depositBankCode(OTHER_BANK_CODE)
                .amount(new BigDecimal("10000"))
                .build();

        DecryptedWithdrawData decryptedData = DecryptedWithdrawData.builder()
                .withdrawalAccountNo("111-111")
                .depositAccountNo("999-999")
                .build();

        given(securityService.decryptWithKey(eq(request), eq(DecryptedWithdrawData.class)))
                .willReturn(new SecurityService.DecryptionResult<>(decryptedData, TEST_CEK));

        TransferResponse mockWithdrawalResponse = TransferResponse.of("mock-tx-id", "2026-06-05 17:00:00", new BigDecimal("90000"));
        given(transferTxService.withdrawTransfer(eq(request), eq(decryptedData), eq(TEST_CEK))).willReturn(mockWithdrawalResponse);

        BankNetworkConfig.BankProperty property = new BankNetworkConfig.BankProperty();
        property.setBaseUrl("http://external-bank");
        given(bankNetworkConfig.getBankProperty(OTHER_BANK_CODE)).willReturn(property);

        // when
        TransferResponse response = transferService.executeTransfer(request);

        // then
        assertNotNull(response);
        verify(restTemplate).postForEntity(eq("http://external-bank/api/v1/baas/transfer/internal/deposit"), any(InternalDepositRequest.class), eq(Object.class));
        verify(transferTxService).updateLedgerStatus("mock-tx-id", "SUCCESS");
    }

    @Test
    @DisplayName("이체 실행: 타행 이체 시 입금 API 실패하고 상태 조회에서 FAILED 확인 시 환불 처리를 진행하고 예외를 발생시킨다")
    void executeTransfer_externalTransfer_depositFailure_refund() {
        // given
        TransferRequest request = TransferRequest.builder()
                .withdrawalBankCode(OUR_BANK_CODE)
                .depositBankCode(OTHER_BANK_CODE)
                .amount(new BigDecimal("10000"))
                .build();

        DecryptedWithdrawData decryptedData = DecryptedWithdrawData.builder()
                .withdrawalAccountNo("111-111")
                .depositAccountNo("999-999")
                .build();

        given(securityService.decryptWithKey(eq(request), eq(DecryptedWithdrawData.class)))
                .willReturn(new SecurityService.DecryptionResult<>(decryptedData, TEST_CEK));

        TransferResponse mockWithdrawalResponse = TransferResponse.of("mock-tx-id", "2026-06-05 17:00:00", new BigDecimal("90000"));
        given(transferTxService.withdrawTransfer(eq(request), eq(decryptedData), eq(TEST_CEK))).willReturn(mockWithdrawalResponse);

        BankNetworkConfig.BankProperty property = new BankNetworkConfig.BankProperty();
        property.setBaseUrl("http://external-bank");
        given(bankNetworkConfig.getBankProperty(OTHER_BANK_CODE)).willReturn(property);

        doThrow(new RuntimeException("Call timed out"))
                .when(restTemplate).postForEntity(anyString(), any(), eq(Object.class));

        Map<String, Object> responseBody = new HashMap<>();
        Map<String, Object> data = new HashMap<>();
        data.put("status", "FAILED");
        responseBody.put("data", data);
        given(restTemplate.getForObject(anyString(), eq(Map.class))).willReturn(responseBody);

        // when & then
        BusinessException ex = assertThrows(BusinessException.class, () ->
                transferService.executeTransfer(request));
        assertEquals(ErrorCode.LOAN_DEPOSIT_BANK_MISMATCH, ex.getErrorCode());
        verify(transferTxService).refundTransfer(eq(request), eq(decryptedData), eq("mock-tx-id"));
    }

    @Test
    @DisplayName("이체 실행: 타행 입금 API 호출 도중 Property 설정 정보를 찾을 수 없을 때 예외가 발생한다")
    void executeTransfer_externalTransfer_propertyNotFound() {
        // given
        TransferRequest request = TransferRequest.builder()
                .withdrawalBankCode(OUR_BANK_CODE)
                .depositBankCode(OTHER_BANK_CODE)
                .amount(new BigDecimal("10000"))
                .build();

        DecryptedWithdrawData decryptedData = DecryptedWithdrawData.builder()
                .withdrawalAccountNo("111-111")
                .depositAccountNo("999-999")
                .build();

        given(securityService.decryptWithKey(eq(request), eq(DecryptedWithdrawData.class)))
                .willReturn(new SecurityService.DecryptionResult<>(decryptedData, TEST_CEK));

        TransferResponse mockWithdrawalResponse = TransferResponse.of("mock-tx-id", "2026-06-05 17:00:00", new BigDecimal("90000"));
        given(transferTxService.withdrawTransfer(eq(request), eq(decryptedData), eq(TEST_CEK))).willReturn(mockWithdrawalResponse);

        given(bankNetworkConfig.getBankProperty(OTHER_BANK_CODE)).willReturn(null);

        // when & then
        BusinessException ex = assertThrows(BusinessException.class, () ->
                transferService.executeTransfer(request));
        assertEquals(ErrorCode.BANK_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("이체 실행: 타행 입금 API 호출 실패 후 거래 상태 확인 시 Property 설정 정보를 찾을 수 없을 때 예외가 발생한다")
    void executeTransfer_externalTransfer_depositCall_propertyNotFound() {
        // given
        TransferRequest request = TransferRequest.builder()
                .withdrawalBankCode(OUR_BANK_CODE)
                .depositBankCode(OTHER_BANK_CODE)
                .amount(new BigDecimal("10000"))
                .build();

        DecryptedWithdrawData decryptedData = DecryptedWithdrawData.builder()
                .withdrawalAccountNo("111-111")
                .depositAccountNo("999-999")
                .build();

        given(securityService.decryptWithKey(eq(request), eq(DecryptedWithdrawData.class)))
                .willReturn(new SecurityService.DecryptionResult<>(decryptedData, TEST_CEK));

        TransferResponse mockWithdrawalResponse = TransferResponse.of("mock-tx-id", "2026-06-05 17:00:00", new BigDecimal("90000"));
        given(transferTxService.withdrawTransfer(eq(request), eq(decryptedData), eq(TEST_CEK))).willReturn(mockWithdrawalResponse);

        BankNetworkConfig.BankProperty property = new BankNetworkConfig.BankProperty();
        property.setBaseUrl("http://external-bank");

        given(bankNetworkConfig.getBankProperty(OTHER_BANK_CODE))
                .willReturn(property)
                .willReturn(null);

        doThrow(new RuntimeException("Call timed out"))
                .when(restTemplate).postForEntity(anyString(), any(), eq(Object.class));

        // when & then
        BusinessException ex = assertThrows(BusinessException.class, () ->
                transferService.executeTransfer(request));
        assertEquals(ErrorCode.BANK_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("이중지급방지: 타행 이체 중 API 에러가 났으나 상대 은행 상태 조회 시 성공으로 확인되면 성공 처리하고 환불을 방지한다")
    void executeTransfer_doublePaymentPrevention_success() {
        // given
        TransferRequest request = TransferRequest.builder()
                .withdrawalBankCode(OUR_BANK_CODE)
                .depositBankCode(OTHER_BANK_CODE)
                .amount(new BigDecimal("10000"))
                .build();

        DecryptedWithdrawData decryptedData = DecryptedWithdrawData.builder()
                .withdrawalAccountNo("111-111")
                .withdrawalPassword("1234")
                .customerRrnPrefix("9001011")
                .depositAccountNo("999-999")
                .build();

        given(securityService.decryptWithKey(any(), eq(DecryptedWithdrawData.class)))
                .willReturn(new SecurityService.DecryptionResult<>(decryptedData, TEST_CEK));
        
        TransferResponse mockWithdrawalResponse = TransferResponse.of("mock-tx-id", "2026-06-05 17:00:00", new BigDecimal("90000"));
        given(transferTxService.withdrawTransfer(eq(request), any(), any())).willReturn(mockWithdrawalResponse);

        BankNetworkConfig.BankProperty bankProperty = new BankNetworkConfig.BankProperty();
        bankProperty.setBaseUrl("http://mock-bank");
        given(bankNetworkConfig.getBankProperty(OTHER_BANK_CODE)).willReturn(bankProperty);

        given(restTemplate.postForEntity(anyString(), any(), eq(Object.class)))
                .willThrow(new ResourceAccessException("Read timed out"));

        Map<String, Object> mockSuccessResponse = new HashMap<>();
        Map<String, Object> mockData = new HashMap<>();
        mockData.put("status", "SUCCESS");
        mockSuccessResponse.put("data", mockData);

        given(restTemplate.getForObject(anyString(), eq(Map.class)))
                .willThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND))
                .willReturn(mockSuccessResponse);

        // when
        TransferResponse response = transferService.executeTransfer(request);

        // then
        assertNotNull(response);
        assertEquals("mock-tx-id", response.getTransactionId());
        verify(transferTxService).updateLedgerStatus("mock-tx-id", "SUCCESS");
        verify(transferTxService, never()).refundTransfer(any(), any(), anyString());
    }

    @Test
    @DisplayName("이중지급방지: 타행 이체 중 API 에러가 나고 상태 조회 API마저 최종 실패(UNKNOWN)하면 예외를 던져 환불을 차단하고 PENDING 상태를 유지한다")
    void executeTransfer_doublePaymentPrevention_unknown() {
        // given
        TransferRequest request = TransferRequest.builder()
                .withdrawalBankCode(OUR_BANK_CODE)
                .depositBankCode(OTHER_BANK_CODE)
                .amount(new BigDecimal("10000"))
                .build();

        DecryptedWithdrawData decryptedData = DecryptedWithdrawData.builder()
                .withdrawalAccountNo("111-111")
                .withdrawalPassword("1234")
                .customerRrnPrefix("9001011")
                .depositAccountNo("999-999")
                .build();

        given(securityService.decryptWithKey(any(), eq(DecryptedWithdrawData.class)))
                .willReturn(new SecurityService.DecryptionResult<>(decryptedData, TEST_CEK));
        
        TransferResponse mockWithdrawalResponse = TransferResponse.of("mock-tx-id", "2026-06-05 17:00:00", new BigDecimal("90000"));
        given(transferTxService.withdrawTransfer(eq(request), any(), any())).willReturn(mockWithdrawalResponse);

        BankNetworkConfig.BankProperty bankProperty = new BankNetworkConfig.BankProperty();
        bankProperty.setBaseUrl("http://mock-bank");
        given(bankNetworkConfig.getBankProperty(OTHER_BANK_CODE)).willReturn(bankProperty);

        given(restTemplate.postForEntity(anyString(), any(), eq(Object.class)))
                .willThrow(new ResourceAccessException("Read timed out"));

        given(restTemplate.getForObject(anyString(), eq(Map.class)))
                .willThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND));

        // when & then
        assertThrows(BusinessException.class, () ->
            transferService.executeTransfer(request)
        );

        verify(transferTxService, never()).updateLedgerStatus(anyString(), anyString());
        verify(transferTxService, never()).refundTransfer(any(), any(), anyString());
    }

    @Test
    @DisplayName("이중지급방지: 타행 이체 중 API 에러가 나고 상태 조회 시 해결 불가능한 400 에러 등이 발생하면 즉시 예외를 던져 재시도를 중단(Fail-Fast)한다")
    void executeTransfer_doublePaymentPrevention_failFast() {
        // given
        TransferRequest request = TransferRequest.builder()
                .withdrawalBankCode(OUR_BANK_CODE)
                .depositBankCode(OTHER_BANK_CODE)
                .amount(new BigDecimal("10000"))
                .build();

        DecryptedWithdrawData decryptedData = DecryptedWithdrawData.builder()
                .withdrawalAccountNo("111-111")
                .withdrawalPassword("1234")
                .customerRrnPrefix("9001011")
                .depositAccountNo("999-999")
                .build();

        given(securityService.decryptWithKey(any(), eq(DecryptedWithdrawData.class)))
                .willReturn(new SecurityService.DecryptionResult<>(decryptedData, TEST_CEK));
        
        TransferResponse mockWithdrawalResponse = TransferResponse.of("mock-tx-id", "2026-06-05 17:00:00", new BigDecimal("90000"));
        given(transferTxService.withdrawTransfer(eq(request), any(), any())).willReturn(mockWithdrawalResponse);

        BankNetworkConfig.BankProperty bankProperty = new BankNetworkConfig.BankProperty();
        bankProperty.setBaseUrl("http://mock-bank");
        given(bankNetworkConfig.getBankProperty(OTHER_BANK_CODE)).willReturn(bankProperty);

        given(restTemplate.postForEntity(anyString(), any(), eq(Object.class)))
                .willThrow(new ResourceAccessException("Read timed out"));

        given(restTemplate.getForObject(anyString(), eq(Map.class)))
                .willThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST));

        // when & then
        assertThrows(BusinessException.class, () ->
            transferService.executeTransfer(request)
        );

        verify(restTemplate, times(1)).getForObject(anyString(), eq(Map.class));
        verify(transferTxService, never()).updateLedgerStatus(anyString(), anyString());
        verify(transferTxService, never()).refundTransfer(any(), any(), anyString());
    }

    @Test
    @DisplayName("이중지급방지: 타행 이체 중 API 에러가 나고 상태 조회 시 예기치 않은 일반 예외가 발생하면 즉시 예외를 던져 재시도를 중단(Fail-Fast)한다")
    void executeTransfer_doublePaymentPrevention_generalExceptionFailFast() {
        // given
        TransferRequest request = TransferRequest.builder()
                .withdrawalBankCode(OUR_BANK_CODE)
                .depositBankCode(OTHER_BANK_CODE)
                .amount(new BigDecimal("10000"))
                .build();

        DecryptedWithdrawData decryptedData = DecryptedWithdrawData.builder()
                .withdrawalAccountNo("111-111")
                .withdrawalPassword("1234")
                .customerRrnPrefix("9001011")
                .depositAccountNo("999-999")
                .build();

        given(securityService.decryptWithKey(any(), eq(DecryptedWithdrawData.class)))
                .willReturn(new SecurityService.DecryptionResult<>(decryptedData, TEST_CEK));
        
        TransferResponse mockWithdrawalResponse = TransferResponse.of("mock-tx-id", "2026-06-05 17:00:00", new BigDecimal("90000"));
        given(transferTxService.withdrawTransfer(eq(request), any(), any())).willReturn(mockWithdrawalResponse);

        BankNetworkConfig.BankProperty bankProperty = new BankNetworkConfig.BankProperty();
        bankProperty.setBaseUrl("http://mock-bank");
        given(bankNetworkConfig.getBankProperty(OTHER_BANK_CODE)).willReturn(bankProperty);

        given(restTemplate.postForEntity(anyString(), any(), eq(Object.class)))
                .willThrow(new ResourceAccessException("Read timed out"));

        given(restTemplate.getForObject(anyString(), eq(Map.class)))
                .willThrow(new RuntimeException("Unexpected DB Connection Failure"));

        // when & then
        assertThrows(BusinessException.class, () ->
            transferService.executeTransfer(request)
        );

        verify(restTemplate, times(1)).getForObject(anyString(), eq(Map.class));
        verify(transferTxService, never()).updateLedgerStatus(anyString(), anyString());
        verify(transferTxService, never()).refundTransfer(any(), any(), anyString());
    }

    @Test
    @DisplayName("거래 상태 조회가 성공한다")
    void getTransferStatus_success() {
        // given
        String txId = "test-tx-id";
        TransactionLedger ledger = TransactionLedger.builder()
                .txId(txId)
                .status("SUCCESS")
                .transactedAt(LocalDateTime.now())
                .build();

        given(transactionLedgerMapper.findByTxId(txId)).willReturn(Optional.of(ledger));

        // when
        TransferStatusResponse response = transferService.getTransferStatus(txId);

        // then
        assertNotNull(response);
        assertEquals(txId, response.getTxId());
        assertEquals("SUCCESS", response.getStatus());
        assertEquals("거래가 조회되었습니다.", response.getMessage());
    }

    @Test
    @DisplayName("거래 상태 조회 시 거래 내역을 찾을 수 없으면 예외가 발생한다")
    void getTransferStatus_notFound() {
        // given
        String txId = "invalid-tx-id";
        given(transactionLedgerMapper.findByTxId(txId)).willReturn(Optional.empty());

        // when & then
        BusinessException ex = assertThrows(BusinessException.class, () ->
                transferService.getTransferStatus(txId));
        assertEquals(ErrorCode.BANK_NOT_FOUND, ex.getErrorCode());
    }
}

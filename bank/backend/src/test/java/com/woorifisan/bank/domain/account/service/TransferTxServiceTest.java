package com.woorifisan.bank.domain.account.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.any;

import com.woorifisan.bank.domain.account.dto.decrypted.DecryptedWithdrawData;
import com.woorifisan.bank.domain.account.dto.request.InternalDepositRequest;
import com.woorifisan.bank.domain.account.dto.request.TransferRequest;
import com.woorifisan.bank.domain.account.dto.response.TransferResponse;
import com.woorifisan.bank.domain.account.mapper.AccountMapper;
import com.woorifisan.bank.domain.account.mapper.TransactionLedgerMapper;
import com.woorifisan.bank.domain.account.model.Account;
import com.woorifisan.bank.domain.customer.service.CustomerService;
import com.woorifisan.bank.global.exception.BusinessException;
import com.woorifisan.bank.global.response.ErrorCode;
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

@ExtendWith(MockitoExtension.class)
class TransferTxServiceTest {

    @InjectMocks
    private TransferTxService transferTxService;

    @Mock
    private AccountMapper accountMapper;

    @Mock
    private CustomerService customerService;

    @Mock
    private TransactionLedgerMapper transactionLedgerMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private SecurityService securityService;

    private Account sender;
    private Account receiver;

    private static final String OUR_BANK_CODE = "020";
    private static final String OTHER_BANK_CODE = "004";
    private static final javax.crypto.SecretKey TEST_CEK = new SecretKeySpec(new byte[16], "AES");

    @BeforeEach
    void setUp() {
        sender = Account.builder()
                .id(1L)
                .customerId(10L)
                .accountNo("111-111")
                .balance(new BigDecimal("100000"))
                .password("hashedPassword")
                .accountType("DEPOSIT")
                .status("NORMAL")
                .version(1)
                .build();

        receiver = Account.builder()
                .id(2L)
                .customerId(20L)
                .accountNo("222-222")
                .balance(new BigDecimal("50000"))
                .password("hashedPassword2")
                .accountType("DEPOSIT")
                .status("NORMAL")
                .version(1)
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

        given(accountMapper.findByAccountNoPlain("111-111")).willReturn(Optional.of(sender));
        given(passwordEncoder.matches("1234", "hashedPassword")).willReturn(true);
        given(accountMapper.findByIdForUpdate(1L)).willReturn(Optional.of(sender));
        given(accountMapper.updateBalance(sender.getId(), new BigDecimal("10000").negate(), sender.getVersion())).willReturn(1);
        given(securityService.encryptResponse(any(), any())).willReturn("encrypted-res");

        // when
        TransferResponse response = transferTxService.withdrawTransfer(request, decryptedData, TEST_CEK);

        // then
        assertNotNull(response);
        assertEquals(0, new BigDecimal("90000").compareTo(response.getBalanceAfter()));
        assertEquals("encrypted-res", response.getResPayload());
        verify(accountMapper).updateBalance(sender.getId(), new BigDecimal("10000").negate(), sender.getVersion());
        verify(transactionLedgerMapper).insert(any());
    }

    @Test
    @DisplayName("출금 이체 시 출금 계좌가 존재하지 않으면 예외가 발생한다")
    void withdrawTransfer_accountNotFound() {
        // given
        TransferRequest request = TransferRequest.builder().amount(new BigDecimal("10000")).build();
        DecryptedWithdrawData decryptedData = DecryptedWithdrawData.builder().withdrawalAccountNo("111-111").build();

        given(accountMapper.findByAccountNoPlain("111-111")).willReturn(Optional.empty());

        // when & then
        BusinessException ex = assertThrows(BusinessException.class, () ->
                transferTxService.withdrawTransfer(request, decryptedData, TEST_CEK));
        assertEquals(ErrorCode.BANK_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("출금 이체 시 고객 본인 인증 실패하면 예외가 발생한다")
    void withdrawTransfer_identificationError() {
        // given
        TransferRequest request = TransferRequest.builder().amount(new BigDecimal("10000")).build();
        DecryptedWithdrawData decryptedData = DecryptedWithdrawData.builder()
                .withdrawalAccountNo("111-111")
                .customerRrnPrefix("9001011")
                .customerName("김철수")
                .build();

        given(accountMapper.findByAccountNoPlain("111-111")).willReturn(Optional.of(sender));
        doThrow(new BusinessException(ErrorCode.IDENTIFICATION_ERROR))
                .when(customerService).verifyCustomerIdentification(10L, "9001011", "김철수");

        // when & then
        BusinessException ex = assertThrows(BusinessException.class, () ->
                transferTxService.withdrawTransfer(request, decryptedData, TEST_CEK));
        assertEquals(ErrorCode.IDENTIFICATION_ERROR, ex.getErrorCode());
    }

    @Test
    @DisplayName("출금 이체 시 계좌 비밀번호가 틀리면 예외가 발생한다")
    void withdrawTransfer_passwordMismatch() {
        // given
        TransferRequest request = TransferRequest.builder().amount(new BigDecimal("10000")).build();
        DecryptedWithdrawData decryptedData = DecryptedWithdrawData.builder()
                .withdrawalAccountNo("111-111")
                .withdrawalPassword("wrongPassword")
                .build();

        given(accountMapper.findByAccountNoPlain("111-111")).willReturn(Optional.of(sender));
        given(passwordEncoder.matches("wrongPassword", "hashedPassword")).willReturn(false);

        // when & then
        BusinessException ex = assertThrows(BusinessException.class, () ->
                transferTxService.withdrawTransfer(request, decryptedData, TEST_CEK));
        assertEquals(ErrorCode.BANK_PW_ERROR, ex.getErrorCode());
    }

    @Test
    @DisplayName("출금 이체 시 출금 계좌의 유형이 DEPOSIT이 아니면 예외가 발생한다")
    void withdrawTransfer_invalidAccountType() {
        // given
        TransferRequest request = TransferRequest.builder().amount(new BigDecimal("10000")).build();
        DecryptedWithdrawData decryptedData = DecryptedWithdrawData.builder()
                .withdrawalAccountNo("111-111")
                .withdrawalPassword("1234")
                .build();

        Account nonDepositSender = Account.builder()
                .id(1L)
                .customerId(10L)
                .accountNo("111-111")
                .balance(new BigDecimal("100000"))
                .password("hashedPassword")
                .accountType("LOAN") // DEPOSIT이 아님
                .status("NORMAL")
                .version(1)
                .build();

        given(accountMapper.findByAccountNoPlain("111-111")).willReturn(Optional.of(nonDepositSender));
        given(passwordEncoder.matches("1234", "hashedPassword")).willReturn(true);

        // when & then
        BusinessException ex = assertThrows(BusinessException.class, () ->
                transferTxService.withdrawTransfer(request, decryptedData, TEST_CEK));
        assertEquals(ErrorCode.INVALID_ACCOUNT_TYPE, ex.getErrorCode());
    }

    @Test
    @DisplayName("출금 이체 시 출금 계좌의 상태가 NORMAL이 아니면 예외가 발생한다")
    void withdrawTransfer_invalidAccountStatus() {
        // given
        TransferRequest request = TransferRequest.builder().amount(new BigDecimal("10000")).build();
        DecryptedWithdrawData decryptedData = DecryptedWithdrawData.builder()
                .withdrawalAccountNo("111-111")
                .withdrawalPassword("1234")
                .build();

        Account abnormalSender = Account.builder()
                .id(1L)
                .customerId(10L)
                .accountNo("111-111")
                .balance(new BigDecimal("100000"))
                .password("hashedPassword")
                .accountType("DEPOSIT")
                .status("LOCKED") // NORMAL이 아님
                .version(1)
                .build();

        given(accountMapper.findByAccountNoPlain("111-111")).willReturn(Optional.of(abnormalSender));
        given(passwordEncoder.matches("1234", "hashedPassword")).willReturn(true);

        // when & then
        BusinessException ex = assertThrows(BusinessException.class, () ->
                transferTxService.withdrawTransfer(request, decryptedData, TEST_CEK));
        assertEquals(ErrorCode.ACCOUNT_NOT_NORMAL, ex.getErrorCode());
    }

    @Test
    @DisplayName("출금 이체 시 최초 조회 잔액이 부족하면 예외가 발생한다")
    void withdrawTransfer_insufficientBalance() {
        // given
        TransferRequest request = TransferRequest.builder().amount(new BigDecimal("150000")).build();
        DecryptedWithdrawData decryptedData = DecryptedWithdrawData.builder()
                .withdrawalAccountNo("111-111")
                .withdrawalPassword("1234")
                .build();

        given(accountMapper.findByAccountNoPlain("111-111")).willReturn(Optional.of(sender));
        given(passwordEncoder.matches("1234", "hashedPassword")).willReturn(true);

        // when & then
        BusinessException ex = assertThrows(BusinessException.class, () ->
                transferTxService.withdrawTransfer(request, decryptedData, TEST_CEK));
        assertEquals(ErrorCode.INSUFFICIENT_BALANCE, ex.getErrorCode());
    }

    @Test
    @DisplayName("출금 이체 시 비관적 락으로 조회한 계좌가 존재하지 않으면 예외가 발생한다")
    void withdrawTransfer_lockAccountNotFound() {
        // given
        TransferRequest request = TransferRequest.builder().amount(new BigDecimal("10000")).build();
        DecryptedWithdrawData decryptedData = DecryptedWithdrawData.builder()
                .withdrawalAccountNo("111-111")
                .withdrawalPassword("1234")
                .build();

        given(accountMapper.findByAccountNoPlain("111-111")).willReturn(Optional.of(sender));
        given(passwordEncoder.matches("1234", "hashedPassword")).willReturn(true);
        given(accountMapper.findByIdForUpdate(1L)).willReturn(Optional.empty());

        // when & then
        BusinessException ex = assertThrows(BusinessException.class, () ->
                transferTxService.withdrawTransfer(request, decryptedData, TEST_CEK));
        assertEquals(ErrorCode.BANK_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("출금 이체 시 비관적 락 조회 후 계좌 유형이 DEPOSIT이 아니면 예외가 발생한다")
    void withdrawTransfer_lockInvalidAccountType() {
        // given
        TransferRequest request = TransferRequest.builder().amount(new BigDecimal("10000")).build();
        DecryptedWithdrawData decryptedData = DecryptedWithdrawData.builder()
                .withdrawalAccountNo("111-111")
                .withdrawalPassword("1234")
                .build();

        Account nonDepositSender = Account.builder()
                .id(1L)
                .customerId(10L)
                .accountNo("111-111")
                .balance(new BigDecimal("100000"))
                .password("hashedPassword")
                .accountType("LOAN") // DEPOSIT이 아님
                .status("NORMAL")
                .version(1)
                .build();

        given(accountMapper.findByAccountNoPlain("111-111")).willReturn(Optional.of(sender));
        given(passwordEncoder.matches("1234", "hashedPassword")).willReturn(true);
        given(accountMapper.findByIdForUpdate(1L)).willReturn(Optional.of(nonDepositSender));

        // when & then
        BusinessException ex = assertThrows(BusinessException.class, () ->
                transferTxService.withdrawTransfer(request, decryptedData, TEST_CEK));
        assertEquals(ErrorCode.INVALID_ACCOUNT_TYPE, ex.getErrorCode());
    }

    @Test
    @DisplayName("출금 이체 시 비관적 락 조회 후 계좌 상태가 NORMAL이 아니면 예외가 발생한다")
    void withdrawTransfer_lockInvalidAccountStatus() {
        // given
        TransferRequest request = TransferRequest.builder().amount(new BigDecimal("10000")).build();
        DecryptedWithdrawData decryptedData = DecryptedWithdrawData.builder()
                .withdrawalAccountNo("111-111")
                .withdrawalPassword("1234")
                .build();

        Account abnormalSender = Account.builder()
                .id(1L)
                .customerId(10L)
                .accountNo("111-111")
                .balance(new BigDecimal("100000"))
                .password("hashedPassword")
                .accountType("DEPOSIT")
                .status("LOCKED") // NORMAL이 아님
                .version(1)
                .build();

        given(accountMapper.findByAccountNoPlain("111-111")).willReturn(Optional.of(sender));
        given(passwordEncoder.matches("1234", "hashedPassword")).willReturn(true);
        given(accountMapper.findByIdForUpdate(1L)).willReturn(Optional.of(abnormalSender));

        // when & then
        BusinessException ex = assertThrows(BusinessException.class, () ->
                transferTxService.withdrawTransfer(request, decryptedData, TEST_CEK));
        assertEquals(ErrorCode.ACCOUNT_NOT_NORMAL, ex.getErrorCode());
    }

    @Test
    @DisplayName("출금 이체 시 비관적 락 조회 후 잔액이 부족하면 예외가 발생한다")
    void withdrawTransfer_lockInsufficientBalance() {
        // given
        TransferRequest request = TransferRequest.builder().amount(new BigDecimal("10000")).build();
        DecryptedWithdrawData decryptedData = DecryptedWithdrawData.builder()
                .withdrawalAccountNo("111-111")
                .withdrawalPassword("1234")
                .build();

        Account poorSender = Account.builder()
                .id(1L)
                .customerId(10L)
                .accountNo("111-111")
                .balance(new BigDecimal("5000")) // 잔액 부족
                .password("hashedPassword")
                .accountType("DEPOSIT")
                .status("NORMAL")
                .version(1)
                .build();

        given(accountMapper.findByAccountNoPlain("111-111")).willReturn(Optional.of(sender));
        given(passwordEncoder.matches("1234", "hashedPassword")).willReturn(true);
        given(accountMapper.findByIdForUpdate(1L)).willReturn(Optional.of(poorSender));

        // when & then
        BusinessException ex = assertThrows(BusinessException.class, () ->
                transferTxService.withdrawTransfer(request, decryptedData, TEST_CEK));
        assertEquals(ErrorCode.INSUFFICIENT_BALANCE, ex.getErrorCode());
    }

    @Test
    @DisplayName("출금 이체 시 낙관적 락 버전 불일치로 업데이트에 실패하면 예외가 발생한다")
    void withdrawTransfer_concurrentModification() {
        // given
        TransferRequest request = TransferRequest.builder().amount(new BigDecimal("10000")).build();
        DecryptedWithdrawData decryptedData = DecryptedWithdrawData.builder()
                .withdrawalAccountNo("111-111")
                .withdrawalPassword("1234")
                .build();

        given(accountMapper.findByAccountNoPlain("111-111")).willReturn(Optional.of(sender));
        given(passwordEncoder.matches("1234", "hashedPassword")).willReturn(true);
        given(accountMapper.findByIdForUpdate(1L)).willReturn(Optional.of(sender));
        given(accountMapper.updateBalance(sender.getId(), new BigDecimal("10000").negate(), sender.getVersion())).willReturn(0);

        // when & then
        BusinessException ex = assertThrows(BusinessException.class, () ->
                transferTxService.withdrawTransfer(request, decryptedData, TEST_CEK));
        assertEquals(ErrorCode.CONCURRENT_MODIFICATION, ex.getErrorCode());
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

        given(accountMapper.findByAccountNoPlain("111-111")).willReturn(Optional.of(sender));
        given(accountMapper.findByIdForUpdate(1L)).willReturn(Optional.of(sender));
        given(accountMapper.updateBalance(sender.getId(), new BigDecimal("10000"), sender.getVersion())).willReturn(1);

        // when
        TransferResponse response = transferTxService.refundTransfer(request, originalData);

        // then
        assertNotNull(response);
        assertEquals(0, new BigDecimal("110000").compareTo(response.getBalanceAfter()));
        verify(accountMapper).updateBalance(sender.getId(), new BigDecimal("10000"), sender.getVersion());
        verify(transactionLedgerMapper).insert(any());
    }

    @Test
    @DisplayName("이체 환불 시 원래 원장 거래 ID가 주어지면 원장 상태를 FAILED로 업데이트하고 환불이 성공한다")
    void refundTransfer_withOriginalTxId() {
        // given
        TransferRequest request = TransferRequest.builder().amount(new BigDecimal("10000")).build();
        DecryptedWithdrawData originalData = DecryptedWithdrawData.builder().withdrawalAccountNo("111-111").build();
        String originalTxId = "orig-tx-id";

        given(accountMapper.findByAccountNoPlain("111-111")).willReturn(Optional.of(sender));
        given(accountMapper.findByIdForUpdate(1L)).willReturn(Optional.of(sender));
        given(accountMapper.updateBalance(sender.getId(), new BigDecimal("10000"), sender.getVersion())).willReturn(1);

        // when
        TransferResponse response = transferTxService.refundTransfer(request, originalData, originalTxId);

        // then
        assertNotNull(response);
        verify(transactionLedgerMapper).updateStatus(originalTxId, "FAILED");
        verify(transactionLedgerMapper).insert(any());
    }

    @Test
    @DisplayName("이체 환불 시 환불받을 계좌가 존재하지 않으면 예외가 발생한다")
    void refundTransfer_accountNotFound() {
        // given
        TransferRequest request = TransferRequest.builder().amount(new BigDecimal("10000")).build();
        DecryptedWithdrawData originalData = DecryptedWithdrawData.builder().withdrawalAccountNo("111-111").build();

        given(accountMapper.findByAccountNoPlain("111-111")).willReturn(Optional.empty());

        // when & then
        BusinessException ex = assertThrows(BusinessException.class, () ->
                transferTxService.refundTransfer(request, originalData));
        assertEquals(ErrorCode.BANK_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("이체 환불 시 비관적 락으로 조회한 계좌가 존재하지 않으면 예외가 발생한다")
    void refundTransfer_lockAccountNotFound() {
        // given
        TransferRequest request = TransferRequest.builder().amount(new BigDecimal("10000")).build();
        DecryptedWithdrawData originalData = DecryptedWithdrawData.builder().withdrawalAccountNo("111-111").build();

        given(accountMapper.findByAccountNoPlain("111-111")).willReturn(Optional.of(sender));
        given(accountMapper.findByIdForUpdate(1L)).willReturn(Optional.empty());

        // when & then
        BusinessException ex = assertThrows(BusinessException.class, () ->
                transferTxService.refundTransfer(request, originalData));
        assertEquals(ErrorCode.BANK_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("이체 환불 시 낙관적 락 버전 불일치로 업데이트에 실패하면 예외가 발생한다")
    void refundTransfer_concurrentModification() {
        // given
        TransferRequest request = TransferRequest.builder().amount(new BigDecimal("10000")).build();
        DecryptedWithdrawData originalData = DecryptedWithdrawData.builder().withdrawalAccountNo("111-111").build();

        given(accountMapper.findByAccountNoPlain("111-111")).willReturn(Optional.of(sender));
        given(accountMapper.findByIdForUpdate(1L)).willReturn(Optional.of(sender));
        given(accountMapper.updateBalance(sender.getId(), new BigDecimal("10000"), sender.getVersion())).willReturn(0);

        // when & then
        BusinessException ex = assertThrows(BusinessException.class, () ->
                transferTxService.refundTransfer(request, originalData));
        assertEquals(ErrorCode.CONCURRENT_MODIFICATION, ex.getErrorCode());
    }

    @Test
    @DisplayName("내부 입금이 성공한다")
    void internalDeposit_success() {
        // given
        InternalDepositRequest request = InternalDepositRequest.builder()
                .depositAccountNo("222-222")
                .amount(new BigDecimal("20000"))
                .withdrawalBankCode(OUR_BANK_CODE)
                .withdrawalAccountNo("111-111")
                .txId("test-tx-id")
                .build();

        given(accountMapper.findByAccountNoPlain("222-222")).willReturn(Optional.of(receiver));
        given(accountMapper.findByIdForUpdate(2L)).willReturn(Optional.of(receiver));
        given(accountMapper.updateBalance(receiver.getId(), new BigDecimal("20000"), receiver.getVersion())).willReturn(1);

        // when
        TransferResponse response = transferTxService.internalDeposit(request);

        // then
        assertNotNull(response);
        assertEquals(0, new BigDecimal("70000").compareTo(response.getBalanceAfter()));
        verify(transactionLedgerMapper).insert(any());
    }

    @Test
    @DisplayName("내부 입금 시 입금받을 계좌가 존재하지 않으면 예외가 발생한다")
    void internalDeposit_accountNotFound() {
        // given
        InternalDepositRequest request = InternalDepositRequest.builder()
                .depositAccountNo("222-222")
                .build();

        given(accountMapper.findByAccountNoPlain("222-222")).willReturn(Optional.empty());

        // when & then
        BusinessException ex = assertThrows(BusinessException.class, () ->
                transferTxService.internalDeposit(request));
        assertEquals(ErrorCode.BANK_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("내부 입금 시 입금받을 계좌의 상태가 NORMAL이 아니면 예외가 발생한다")
    void internalDeposit_invalidAccountStatus() {
        // given
        InternalDepositRequest request = InternalDepositRequest.builder()
                .depositAccountNo("222-222")
                .build();

        Account abnormalReceiver = Account.builder()
                .id(2L)
                .customerId(20L)
                .accountNo("222-222")
                .balance(new BigDecimal("50000"))
                .password("hashedPassword2")
                .accountType("DEPOSIT")
                .status("LOCKED") // NORMAL이 아님
                .version(1)
                .build();

        given(accountMapper.findByAccountNoPlain("222-222")).willReturn(Optional.of(abnormalReceiver));

        // when & then
        BusinessException ex = assertThrows(BusinessException.class, () ->
                transferTxService.internalDeposit(request));
        assertEquals(ErrorCode.ACCOUNT_NOT_NORMAL, ex.getErrorCode());
    }

    @Test
    @DisplayName("내부 입금 시 비관적 락으로 조회한 계좌가 존재하지 않으면 예외가 발생한다")
    void internalDeposit_lockAccountNotFound() {
        // given
        InternalDepositRequest request = InternalDepositRequest.builder()
                .depositAccountNo("222-222")
                .build();

        given(accountMapper.findByAccountNoPlain("222-222")).willReturn(Optional.of(receiver));
        given(accountMapper.findByIdForUpdate(2L)).willReturn(Optional.empty());

        // when & then
        BusinessException ex = assertThrows(BusinessException.class, () ->
                transferTxService.internalDeposit(request));
        assertEquals(ErrorCode.BANK_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("내부 입금 시 비관적 락 조회 후 계좌 상태가 NORMAL이 아니면 예외가 발생한다")
    void internalDeposit_lockInvalidAccountStatus() {
        // given
        InternalDepositRequest request = InternalDepositRequest.builder()
                .depositAccountNo("222-222")
                .build();

        Account abnormalReceiver = Account.builder()
                .id(2L)
                .customerId(20L)
                .accountNo("222-222")
                .balance(new BigDecimal("50000"))
                .password("hashedPassword2")
                .accountType("DEPOSIT")
                .status("LOCKED") // NORMAL이 아님
                .version(1)
                .build();

        given(accountMapper.findByAccountNoPlain("222-222")).willReturn(Optional.of(receiver));
        given(accountMapper.findByIdForUpdate(2L)).willReturn(Optional.of(abnormalReceiver));

        // when & then
        BusinessException ex = assertThrows(BusinessException.class, () ->
                transferTxService.internalDeposit(request));
        assertEquals(ErrorCode.ACCOUNT_NOT_NORMAL, ex.getErrorCode());
    }

    @Test
    @DisplayName("내부 입금 시 낙관적 락 버전 불일치로 업데이트에 실패하면 예외가 발생한다")
    void internalDeposit_concurrentModification() {
        // given
        InternalDepositRequest request = InternalDepositRequest.builder()
                .depositAccountNo("222-222")
                .amount(new BigDecimal("20000"))
                .build();

        given(accountMapper.findByAccountNoPlain("222-222")).willReturn(Optional.of(receiver));
        given(accountMapper.findByIdForUpdate(2L)).willReturn(Optional.of(receiver));
        given(accountMapper.updateBalance(receiver.getId(), new BigDecimal("20000"), receiver.getVersion())).willReturn(0);

        // when & then
        BusinessException ex = assertThrows(BusinessException.class, () ->
                transferTxService.internalDeposit(request));
        assertEquals(ErrorCode.CONCURRENT_MODIFICATION, ex.getErrorCode());
    }

    @Test
    @DisplayName("거래 원장의 상태를 성공적으로 업데이트한다")
    void updateLedgerStatus_success() {
        // when
        transferTxService.updateLedgerStatus("tx-id", "SUCCESS");

        // then
        verify(transactionLedgerMapper).updateStatus("tx-id", "SUCCESS");
    }
}

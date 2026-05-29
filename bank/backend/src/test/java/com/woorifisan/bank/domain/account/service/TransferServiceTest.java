package com.woorifisan.bank.domain.account.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

import com.woorifisan.bank.domain.account.dto.decrypted.DecryptedDepositData;
import com.woorifisan.bank.domain.account.dto.decrypted.DecryptedWithdrawData;
import com.woorifisan.bank.domain.account.dto.request.DepositRequest;
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

    private Account sender;
    private Account receiver;
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
        given(securityService.encryptResponse(any(), any())).willReturn("encrypted-res");

        // when
        TransferResponse response = transferService.withdrawTransfer(request);

        // then
        assertNotNull(response);
        assertEquals(new BigDecimal("90000"), response.getBalanceAfter());
        assertEquals("encrypted-res", response.getResPayload());
        verify(accountMapper).updateBalance(sender.getId(), new BigDecimal("10000").negate());
        verify(transactionLedgerMapper).insert(any());
    }

    @Test
    @DisplayName("입금 이체(타행입금 포함)가 성공한다")
    void depositTransfer_success() {
        // given
        DepositRequest request = DepositRequest.builder()
                .withdrawalBankCode(OTHER_BANK_CODE)
                .amount(new BigDecimal("10000"))
                .reqPayload("encrypted-jwe")
                .bankKeyId("key-id")
                .build();

        DecryptedDepositData decryptedData = DecryptedDepositData.builder()
                .depositAccountNo("222-222")
                .withdrawalAccountNo("888-888")
                .build();

        given(securityService.decryptWithKey(any(), eq(DecryptedDepositData.class)))
                .willReturn(new SecurityService.DecryptionResult<>(decryptedData, new SecretKeySpec(new byte[16], "AES")));

        given(accountMapper.findByAccountNoPlain("222-222")).willReturn(Optional.of(receiver));
        given(accountMapper.findByIdForUpdate(2L)).willReturn(Optional.of(receiver));
        given(securityService.encryptResponse(any(), any())).willReturn("encrypted-res");

        // when
        TransferResponse response = transferService.depositTransfer(request);

        // then
        assertNotNull(response);
        assertEquals(new BigDecimal("60000"), response.getBalanceAfter());
        verify(accountMapper).updateBalance(receiver.getId(), new BigDecimal("10000"));
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

        // when
        TransferResponse response = transferService.refundTransfer(request);

        // then
        assertNotNull(response);
        assertEquals(new BigDecimal("110000"), response.getBalanceAfter());
        verify(accountMapper).updateBalance(sender.getId(), new BigDecimal("10000"));
        verify(transactionLedgerMapper).insert(any());
    }
}

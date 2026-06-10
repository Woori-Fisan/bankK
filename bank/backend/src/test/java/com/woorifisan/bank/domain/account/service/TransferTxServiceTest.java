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
import com.woorifisan.bank.domain.customer.service.CustomerService;
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
    private CustomerMapper customerMapper;

    @Mock
    private CustomerService customerService;

    @Mock
    private TransactionLedgerMapper transactionLedgerMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private SecurityService securityService;

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

        javax.crypto.SecretKey cek = new javax.crypto.spec.SecretKeySpec(new byte[16], "AES");
        
        given(accountMapper.findByAccountNoPlain("111-111")).willReturn(Optional.of(sender));
        given(passwordEncoder.matches("1234", "hashedPassword")).willReturn(true);
        given(accountMapper.findByIdForUpdate(1L)).willReturn(Optional.of(sender));
        given(accountMapper.updateBalance(sender.getId(), new BigDecimal("10000").negate())).willReturn(1);
        given(securityService.encryptResponse(any(), any())).willReturn("encrypted-res");

        // when
        TransferResponse response = transferTxService.withdrawTransfer(request, decryptedData, cek);

        // then
        assertNotNull(response);
        assertEquals(new BigDecimal("90000"), response.getBalanceAfter());
        assertEquals("encrypted-res", response.getResPayload());
        verify(accountMapper).updateBalance(sender.getId(), new BigDecimal("10000").negate());
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

        given(accountMapper.findByAccountNoPlain("111-111")).willReturn(Optional.of(sender));
        given(accountMapper.findByIdForUpdate(1L)).willReturn(Optional.of(sender));
        given(accountMapper.updateBalance(sender.getId(), new BigDecimal("10000"))).willReturn(1);

        // when
        TransferResponse response = transferTxService.refundTransfer(request, originalData);

        // then
        assertNotNull(response);
        assertEquals(new BigDecimal("110000"), response.getBalanceAfter());
        verify(accountMapper).updateBalance(sender.getId(), new BigDecimal("10000"));
        verify(transactionLedgerMapper).insert(any());
    }
}

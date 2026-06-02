package com.woorifisan.bank.domain.account.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.any;

import com.woorifisan.bank.domain.account.dto.request.DepositRequest;
import com.woorifisan.bank.domain.account.dto.request.TransferRequest;
import com.woorifisan.bank.domain.account.dto.response.TransferResponse;
import com.woorifisan.bank.domain.account.mapper.AccountMapper;
import com.woorifisan.bank.domain.account.mapper.TransactionLedgerMapper;
import com.woorifisan.bank.domain.account.model.Account;
import com.woorifisan.bank.domain.customer.mapper.CustomerMapper;
import com.woorifisan.bank.domain.customer.model.Customer;
import com.woorifisan.bank.global.exception.BusinessException;
import com.woorifisan.bank.global.response.ErrorCode;
import java.math.BigDecimal;
import java.util.Optional;
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

    // --- 성공 케이스 ---

    @Test
    @DisplayName("당행이체가 성공한다")
    void 당행이체가_성공한다() {
        // given
        TransferRequest request = createTransferRequest(OUR_BANK_CODE, "10000");

        given(accountMapper.findByAccountNoPlain("111-111")).willReturn(Optional.of(sender));
        given(customerMapper.findById(10L)).willReturn(Optional.of(senderCustomer));
        given(passwordEncoder.matches("1234", "hashedPassword")).willReturn(true);
        given(accountMapper.findByAccountNoPlain("222-222")).willReturn(Optional.of(receiver));
        
        // Locking (sender.id < receiver.id)
        given(accountMapper.findByIdForUpdate(1L)).willReturn(Optional.of(sender));
        given(accountMapper.findByIdForUpdate(2L)).willReturn(Optional.of(receiver));
        given(accountMapper.updateBalance(any(), any(), any())).willReturn(1);

        // when
        TransferResponse response = transferService.executeTransfer(request);

        // then
        assertNotNull(response);
        assertEquals(new BigDecimal("90000"), response.getBalanceAfter());
        // Delta values
        verify(accountMapper).updateBalance(sender.getId(), new BigDecimal("10000").negate(), sender.getVersion());
        verify(accountMapper).updateBalance(receiver.getId(), new BigDecimal("10000"), receiver.getVersion());
        verify(transactionLedgerMapper, org.mockito.Mockito.times(2)).insert(any());
    }

    @Test
    @DisplayName("타행출금이 성공한다")
    void 타행출금이_성공한다() {
        // given
        TransferRequest request = createTransferRequest(OTHER_BANK_CODE, "10000");

        given(accountMapper.findByAccountNoPlain("111-111")).willReturn(Optional.of(sender));
        given(customerMapper.findById(10L)).willReturn(Optional.of(senderCustomer));
        given(passwordEncoder.matches("1234", "hashedPassword")).willReturn(true);
        given(accountMapper.findByIdForUpdate(1L)).willReturn(Optional.of(sender));
        given(accountMapper.updateBalance(any(), any(), any())).willReturn(1);

        // when
        TransferResponse response = transferService.withdrawTransfer(request);

        // then
        assertNotNull(response);
        assertEquals(new BigDecimal("90000"), response.getBalanceAfter());
        // Delta values
        verify(accountMapper).updateBalance(sender.getId(), new BigDecimal("10000").negate(), sender.getVersion());
    }

    @Test
    @DisplayName("타행입금이 성공한다")
    void 타행입금이_성공한다() {
        // given
        DepositRequest request = DepositRequest.builder()
                .depositAccountNo("222-222")
                .amount(new BigDecimal("10000"))
                .withdrawalBankCode(OTHER_BANK_CODE)
                .withdrawalAccountNo("333-333")
                .build();

        given(accountMapper.findByAccountNoPlain("222-222")).willReturn(Optional.of(receiver));
        given(accountMapper.findByIdForUpdate(2L)).willReturn(Optional.of(receiver));
        given(accountMapper.updateBalance(any(), any(), any())).willReturn(1);

        // when
        TransferResponse response = transferService.depositTransfer(request);

        // then
        assertNotNull(response);
        assertEquals(new BigDecimal("60000"), response.getBalanceAfter());
        // Delta values
        verify(accountMapper).updateBalance(receiver.getId(), new BigDecimal("10000"), receiver.getVersion());
    }

    // --- 실패 케이스 (에러코드 검증) ---

    @Test
    @DisplayName("당행이체 시 타행코드가 오면 예외가 발생한다(INVALID_INPUT)")
    void 당행이체_시_타행코드가_오면_예외가_발생한다() {
        // given
        TransferRequest request = createTransferRequest(OTHER_BANK_CODE, "10000");

        // when & then
        BusinessException ex = assertThrows(BusinessException.class, () -> transferService.executeTransfer(request));
        assertEquals(ErrorCode.INVALID_INPUT, ex.getErrorCode());
    }

    @Test
    @DisplayName("출금계좌가 없으면 예외가 발생한다(BANK_NOT_FOUND)")
    void 출금계좌가_없으면_예외가_발생한다() {
        // given
        TransferRequest request = createTransferRequest(OUR_BANK_CODE, "10000");
        given(accountMapper.findByAccountNoPlain("111-111")).willReturn(Optional.empty());

        // when & then
        BusinessException ex = assertThrows(BusinessException.class, () -> transferService.executeTransfer(request));
        assertEquals(ErrorCode.BANK_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("주민번호가 다르면 예외가 발생한다(TRANSFER_002)")
    void 주민번호가_다르면_예외가_발생한다() {
        // given
        TransferRequest request = TransferRequest.builder()
                .withdrawalAccountNo("111-111")
                .withdrawalPassword("1234")
                .customerRrnPrefix("9999999") // 잘못된 주민번호
                .depositBankCode(OUR_BANK_CODE)
                .depositAccountNo("222-222")
                .amount(new BigDecimal("10000"))
                .build();

        given(accountMapper.findByAccountNoPlain("111-111")).willReturn(Optional.of(sender));
        given(customerMapper.findById(10L)).willReturn(Optional.of(senderCustomer));

        // when & then
        BusinessException ex = assertThrows(BusinessException.class, () -> transferService.executeTransfer(request));
        assertEquals(ErrorCode.IDENTIFICATION_ERROR, ex.getErrorCode());
    }

    @Test
    @DisplayName("비밀번호가 틀리면 예외가 발생한다(BANK_PW_ERROR)")
    void 비밀번호가_틀리면_예외가_발생한다() {
        // given
        TransferRequest request = createTransferRequest(OUR_BANK_CODE, "10000");
        given(accountMapper.findByAccountNoPlain("111-111")).willReturn(Optional.of(sender));
        given(customerMapper.findById(10L)).willReturn(Optional.of(senderCustomer));
        given(passwordEncoder.matches("1234", "hashedPassword")).willReturn(false); // 비밀번호 불일치

        // when & then
        BusinessException ex = assertThrows(BusinessException.class, () -> transferService.executeTransfer(request));
        assertEquals(ErrorCode.BANK_PW_ERROR, ex.getErrorCode());
    }

    @Test
    @DisplayName("락 획득 후 잔액이 부족하면 예외가 발생한다(TRANSFER_001)")
    void 잔액이_부족하면_예외가_발생한다() {
        // given
        TransferRequest request = createTransferRequest(OUR_BANK_CODE, "500000"); // 50만 (잔액 10만)
        given(accountMapper.findByAccountNoPlain("111-111")).willReturn(Optional.of(sender));
        given(customerMapper.findById(10L)).willReturn(Optional.of(senderCustomer));
        given(passwordEncoder.matches("1234", "hashedPassword")).willReturn(true);
        given(accountMapper.findByAccountNoPlain("222-222")).willReturn(Optional.of(receiver));
        
        // Locking
        given(accountMapper.findByIdForUpdate(1L)).willReturn(Optional.of(sender));
        given(accountMapper.findByIdForUpdate(2L)).willReturn(Optional.of(receiver));

        // when & then
        BusinessException ex = assertThrows(BusinessException.class, () -> transferService.executeTransfer(request));
        assertEquals(ErrorCode.INSUFFICIENT_BALANCE, ex.getErrorCode());
    }

    private TransferRequest createTransferRequest(String bankCode, String amount) {
        return TransferRequest.builder()
                .withdrawalAccountNo("111-111")
                .withdrawalPassword("1234")
                .customerRrnPrefix("9001011")
                .depositBankCode(bankCode)
                .depositAccountNo("222-222")
                .amount(new BigDecimal(amount))
                .build();
    }
}

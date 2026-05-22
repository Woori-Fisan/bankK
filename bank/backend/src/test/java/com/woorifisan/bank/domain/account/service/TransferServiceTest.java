package com.woorifisan.bank.domain.account.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;

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
import com.woorifisan.bank.global.util.CryptoUtil;
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
    private CryptoUtil cryptoUtil;

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

    @BeforeEach
    void setUp() {
        sender = Account.builder()
                .id(1L)
                .customerId(10L)
                .balance(new BigDecimal("100000"))
                .passwordHash("hashedPassword")
                .build();

        receiver = Account.builder()
                .id(2L)
                .customerId(20L)
                .balance(new BigDecimal("50000"))
                .build();

        senderCustomer = Customer.builder()
                .id(10L)
                .rrnPrefixEnc("encryptedRrnPrefix")
                .build();
    }

    // --- 성공 케이스 ---

    @Test
    @DisplayName("당행이체가_성공한다")
    void 당행이체가_성공한다() {
        // given
        TransferRequest request = createTransferRequest("040", "10000");

        given(cryptoUtil.hash(anyString())).willReturn("hashedNo");
        given(accountMapper.findByAccountNoHashWithLock("hashedNo"))
                .willReturn(Optional.of(sender))
                .willReturn(Optional.of(receiver));
        
        given(customerMapper.findById(10L)).willReturn(Optional.of(senderCustomer));
        given(cryptoUtil.decrypt("encryptedRrnPrefix")).willReturn("9001011");
        given(passwordEncoder.matches("1234", "hashedPassword")).willReturn(true);
        given(cryptoUtil.encrypt(anyString())).willReturn("encryptedTargetAccount");

        // when
        TransferResponse response = transferService.executeTransfer(request);

        // then
        assertNotNull(response);
        assertEquals(new BigDecimal("90000"), response.getBalanceAfter());
        verify(accountMapper).updateBalance(sender.getId(), new BigDecimal("90000"));
        verify(accountMapper).updateBalance(receiver.getId(), new BigDecimal("60000"));
    }

    @Test
    @DisplayName("타행출금이_성공한다")
    void 타행출금이_성공한다() {
        // given
        TransferRequest request = createTransferRequest("081", "10000");

        given(cryptoUtil.hash("111-111")).willReturn("hashedNo");
        given(accountMapper.findByAccountNoHashWithLock("hashedNo")).willReturn(Optional.of(sender));
        given(customerMapper.findById(10L)).willReturn(Optional.of(senderCustomer));
        given(cryptoUtil.decrypt("encryptedRrnPrefix")).willReturn("9001011");
        given(passwordEncoder.matches("1234", "hashedPassword")).willReturn(true);
        given(cryptoUtil.encrypt(anyString())).willReturn("encryptedTargetAccount");

        // when
        TransferResponse response = transferService.withdrawTransfer(request);

        // then
        assertNotNull(response);
        assertEquals(new BigDecimal("90000"), response.getBalanceAfter());
        verify(accountMapper).updateBalance(sender.getId(), new BigDecimal("90000"));
    }

    @Test
    @DisplayName("타행입금이_성공한다")
    void 타행입금이_성공한다() {
        // given
        DepositRequest request = DepositRequest.builder()
                .depositAccountNo("222-222")
                .amount(new BigDecimal("10000"))
                .withdrawalBankCode("081")
                .withdrawalAccountNo("333-333")
                .build();

        given(cryptoUtil.hash("222-222")).willReturn("hashedNo");
        given(accountMapper.findByAccountNoHashWithLock("hashedNo")).willReturn(Optional.of(receiver));
        given(cryptoUtil.encrypt(anyString())).willReturn("encryptedTargetAccount");

        // when
        TransferResponse response = transferService.depositTransfer(request);

        // then
        assertNotNull(response);
        assertEquals(new BigDecimal("60000"), response.getBalanceAfter());
        verify(accountMapper).updateBalance(receiver.getId(), new BigDecimal("60000"));
    }

    // --- 실패 케이스 (에러코드 검증) ---

    @Test
    @DisplayName("당행이체_시_타행코드가_오면_예외가_발생한다(INVALID_INPUT)")
    void 당행이체_시_타행코드가_오면_예외가_발생한다() {
        // given
        TransferRequest request = createTransferRequest("081", "10000");

        // when & then
        BusinessException ex = assertThrows(BusinessException.class, () -> transferService.executeTransfer(request));
        assertEquals(ErrorCode.INVALID_INPUT, ex.getErrorCode());
    }

    @Test
    @DisplayName("출금계좌가_없으면_예외가_발생한다(BANK_NOT_FOUND)")
    void 출금계좌가_없으면_예외가_발생한다() {
        // given
        TransferRequest request = createTransferRequest("040", "10000");
        given(cryptoUtil.hash(anyString())).willReturn("hashedNo");
        given(accountMapper.findByAccountNoHashWithLock("hashedNo")).willReturn(Optional.empty());

        // when & then
        BusinessException ex = assertThrows(BusinessException.class, () -> transferService.executeTransfer(request));
        assertEquals(ErrorCode.BANK_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("주민번호가_다르면_예외가_발생한다(TRANSFER_002)")
    void 주민번호가_다르면_예외가_발생한다() {
        // given
        TransferRequest request = TransferRequest.builder()
                .withdrawalAccountNo("111-111")
                .withdrawalPassword("1234")
                .customerRrnPrefix("999999") // 잘못된 주민번호
                .depositBankCode("040")
                .depositAccountNo("222-222")
                .amount(new BigDecimal("10000"))
                .build();

        given(cryptoUtil.hash(anyString())).willReturn("hashedNo");
        given(accountMapper.findByAccountNoHashWithLock("hashedNo")).willReturn(Optional.of(sender));
        given(customerMapper.findById(10L)).willReturn(Optional.of(senderCustomer));
        given(cryptoUtil.decrypt("encryptedRrnPrefix")).willReturn("9001011");

        // when & then
        BusinessException ex = assertThrows(BusinessException.class, () -> transferService.executeTransfer(request));
        assertEquals(ErrorCode.IDENTIFICATION_ERROR, ex.getErrorCode());
    }

    @Test
    @DisplayName("비밀번호가_틀리면_예외가_발생한다(BANK_PW_ERROR)")
    void 비밀번호가_틀리면_예외가_발생한다() {
        // given
        TransferRequest request = createTransferRequest("040", "10000");
        given(cryptoUtil.hash(anyString())).willReturn("hashedNo");
        given(accountMapper.findByAccountNoHashWithLock("hashedNo")).willReturn(Optional.of(sender));
        given(customerMapper.findById(10L)).willReturn(Optional.of(senderCustomer));
        given(cryptoUtil.decrypt("encryptedRrnPrefix")).willReturn("9001011");
        given(passwordEncoder.matches("1234", "hashedPassword")).willReturn(false); // 비밀번호 불일치

        // when & then
        BusinessException ex = assertThrows(BusinessException.class, () -> transferService.executeTransfer(request));
        assertEquals(ErrorCode.BANK_PW_ERROR, ex.getErrorCode());
    }

    @Test
    @DisplayName("잔액이_부족하면_예외가_발생한다(TRANSFER_001)")
    void 잔액이_부족하면_예외가_발생한다() {
        // given
        TransferRequest request = createTransferRequest("040", "500000"); // 50만 (잔액 10만)
        given(cryptoUtil.hash(anyString())).willReturn("hashedNo");
        given(accountMapper.findByAccountNoHashWithLock("hashedNo")).willReturn(Optional.of(sender));
        given(customerMapper.findById(10L)).willReturn(Optional.of(senderCustomer));
        given(cryptoUtil.decrypt("encryptedRrnPrefix")).willReturn("9001011");
        given(passwordEncoder.matches("1234", "hashedPassword")).willReturn(true);

        // when & then
        BusinessException ex = assertThrows(BusinessException.class, () -> transferService.executeTransfer(request));
        assertEquals(ErrorCode.INSUFFICIENT_BALANCE, ex.getErrorCode());
    }

    @Test
    @DisplayName("입금계좌가_없으면_예외가_발생한다(BANK_NOT_FOUND)")
    void 입금계좌가_없으면_예외가_발생한다() {
        // given
        TransferRequest request = createTransferRequest("040", "10000");
        given(cryptoUtil.hash(anyString())).willReturn("hashedNo");
        given(accountMapper.findByAccountNoHashWithLock("hashedNo"))
                .willReturn(Optional.of(sender)) // 출금계좌 있음
                .willReturn(Optional.empty());  // 입금계좌 없음
        
        given(customerMapper.findById(10L)).willReturn(Optional.of(senderCustomer));
        given(cryptoUtil.decrypt("encryptedRrnPrefix")).willReturn("9001011");
        given(passwordEncoder.matches("1234", "hashedPassword")).willReturn(true);

        // when & then
        BusinessException ex = assertThrows(BusinessException.class, () -> transferService.executeTransfer(request));
        assertEquals(ErrorCode.BANK_NOT_FOUND, ex.getErrorCode());
    }

    private TransferRequest createTransferRequest(String bankCode, String amount) {
        return TransferRequest.builder()
                .withdrawalAccountNo("111-111")
                .withdrawalPassword("1234")
                .customerRrnPrefix("900101")
                .depositBankCode(bankCode)
                .depositAccountNo("222-222")
                .amount(new BigDecimal(amount))
                .build();
    }
}

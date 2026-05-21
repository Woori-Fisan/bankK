package com.woorifisan.bank.domain.account.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 거래 원장 도메인 모델
 */
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class TransactionLedger {

    private String txId;            // 거래 고유 ID (PK)
    private Long accountId;         // 거래 발생 계좌 ID (FK)
    private String txType;          // 거래 유형 (DEPOSIT, WITHDRAW, TRANSFER, LOAN)
    private BigDecimal amount;      // 거래 금액
    private BigDecimal balanceAfter; // 거래 직후 잔액 스냅샷
    private String targetBankCode;  // 타행 이체 시 상대 은행 코드
    private String targetAccount;   // 타행 이체 시 상대 계좌번호 (AES-256 암호화)
    private String description;     // 통장 인자 내용 (적요)
    private String status;          // 거래 상태 (SUCCESS, FAILED, PENDING)
    private LocalDateTime transactedAt; // 거래 일시
    private LocalDateTime createdAt;    // 생성 일시

    /**
     * 신규 거래 내역 생성을 위한 정적 팩토리 메서드
     */
    public static TransactionLedger of(String txId, Long accountId, String txType, BigDecimal amount, 
                                     BigDecimal balanceAfter, String description, String status) {
        return TransactionLedger.builder()
                .txId(txId)
                .accountId(accountId)
                .txType(txType)
                .amount(amount)
                .balanceAfter(balanceAfter)
                .description(description)
                .status(status != null ? status : "SUCCESS")
                .transactedAt(LocalDateTime.now())
                .build();
    }
}

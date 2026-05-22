package com.woorifisan.bank.domain.account.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 이체 실행 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class TransferResponse {

    private String transactionId;

    private String transactionDate;

    private BigDecimal balanceAfter;

    public static TransferResponse of(String transactionId, String transactionDate, BigDecimal balanceAfter) {
        return TransferResponse.builder()
                .transactionId(transactionId)
                .transactionDate(transactionDate)
                .balanceAfter(balanceAfter)
                .build();
    }

    /**
     * 테스트용 임시 응답 생성
     */
    public static TransferResponse mock(BigDecimal balanceAfter) {
        return TransferResponse.builder()
                .transactionId(UUID.randomUUID().toString())
                .transactionDate(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                .balanceAfter(balanceAfter)
                .build();
    }
}

package com.woorifisan.bank.domain.account.dto.response;

import com.woorifisan.bank.global.security.dto.SecureResponse;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * 이체 실행 응답 DTO (E2EE 적용)
 */
@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class TransferResponse extends SecureResponse {

    private String transactionId;

    private String transactionDate;

    private BigDecimal balanceAfter;

    /**
     * 암호화될 민감 데이터 구조 정의
     */
    @Getter
    @Builder
    @AllArgsConstructor
    public static class SensitiveData {
        private BigDecimal balanceAfter;
    }

    public static TransferResponse of(String transactionId, String transactionDate, BigDecimal balanceAfter) {
        return TransferResponse.builder()
                .transactionId(transactionId)
                .transactionDate(transactionDate)
                .balanceAfter(balanceAfter)
                .build();
    }

    public static TransferResponse of(String transactionId, String transactionDate, BigDecimal balanceAfter, String resPayload) {
        return TransferResponse.builder()
                .transactionId(transactionId)
                .transactionDate(transactionDate)
                .balanceAfter(balanceAfter)
                .resPayload(resPayload)
                .build();
    }
}

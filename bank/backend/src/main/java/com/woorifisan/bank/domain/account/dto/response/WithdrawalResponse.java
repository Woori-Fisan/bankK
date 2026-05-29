package com.woorifisan.bank.domain.account.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * 현금 출금 응답 DTO
 */
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Schema(description = "현금 출금 응답 정보")
public class WithdrawalResponse {

    @Schema(description = "거래 명세 고유 ID (원장 번호)", example = "TX-20260521-001")
    private String transactionId;

    @Schema(description = "현금 출금 후 가용 잔액", example = "1450000")
    private BigDecimal balanceAfter;

    @Schema(description = "거래 확정 일시 (ISO-8601)", example = "2026-05-21T14:30:00Z")
    private String transactionDate;

}

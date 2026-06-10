package com.woorifisan.bank.domain.account.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Schema(description = "개별 거래 내역 항목 DTO")
public class TransactionHistoryDto {
    @Schema(description = "거래 고유 ID", example = "TX123456")
    private String txId;

    @Schema(description = "거래 일시", example = "2024-01-15 14:30:05")
    private String txDate;

    @Schema(description = "거래 유형", example = "WITHDRAW")
    private String txType;

    @Schema(description = "거래 금액", example = "50000")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private BigDecimal amount;

    @Schema(description = "거래 후 잔액", example = "150000")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private BigDecimal balance;

    @Schema(description = "상대방 성명/계좌", example = "홍길동")
    private String counterpartName;

    @Schema(description = "거래 메모/적요", example = "ATM 출금")
    private String description;
}

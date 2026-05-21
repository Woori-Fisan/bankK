package com.woorifisan.bank.domain.account.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * 잔액 조회 응답 DTO
 */
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Schema(description = "잔액 조회 응답 정보")
public class BalanceInquiryResponse {

    @Schema(description = "현재 잔액", example = "150000")
    private BigDecimal balance;

    @Schema(description = "계좌 상태 (NORMAL, LOCKED, CLOSED)", example = "NORMAL")
    private String status;

}

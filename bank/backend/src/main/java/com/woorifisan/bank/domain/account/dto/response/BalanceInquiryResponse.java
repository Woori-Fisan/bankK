package com.woorifisan.bank.domain.account.dto.response;

import com.woorifisan.bank.global.security.dto.SecureResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * 잔액 조회 응답 DTO
 */
@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Schema(description = "잔액 조회 응답 정보")
public class BalanceInquiryResponse extends SecureResponse {

    @Schema(description = "계좌 상태 (NORMAL, LOCKED, CLOSED)", example = "NORMAL")
    private String status;

    /**
     * 암호화될 민감 데이터 구조
     */
    @Getter
    @Builder
    @AllArgsConstructor
    public static class SensitiveData {
        private BigDecimal balance;
    }

    public static BalanceInquiryResponse of(String resPayload, String status) {
        return BalanceInquiryResponse.builder()
                .resPayload(resPayload)
                .status(status)
                .build();
    }

}

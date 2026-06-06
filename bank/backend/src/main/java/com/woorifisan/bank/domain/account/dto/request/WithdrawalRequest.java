package com.woorifisan.bank.domain.account.dto.request;

import com.woorifisan.bank.global.security.dto.SecureRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * 현금 출금 요청 DTO
 */
@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Schema(description = "현금 출금 요청 정보")
public class WithdrawalRequest extends SecureRequest {

    @NotNull(message = "출금 요청 금액은 필수입니다.")
    @Positive(message = "출금 요청 금액은 0보다 커야 합니다.")
    @Schema(description = "출금 요청 금액", example = "50000")
    private BigDecimal amount;

}

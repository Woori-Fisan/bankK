package com.woorifisan.platform.domain.bank.dto.request;

import com.woorifisan.platform.global.security.dto.SecureRequest;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

/**
 * 출금 실행 요청 DTO (E2EE 적용)
 */
@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class WithdrawalRequest extends SecureRequest {

    @NotBlank(message = "출금 은행 암호화 페이로드는 필수입니다.")
    @Override
    public String getReqPayload() {
        return super.getReqPayload();
    }

    @NotBlank(message = "출금 은행 코드는 필수입니다.")
    private String withdrawalBankCode;

    @NotNull(message = "출금 금액은 필수입니다.")
    @Positive(message = "출금 금액은 0보다 커야 합니다.")
    private BigDecimal amount;

}

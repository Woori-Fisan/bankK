package com.woorifisan.platform.domain.bank.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

import com.woorifisan.platform.global.security.dto.SecureRequest;
import lombok.experimental.SuperBuilder;

/**
 * 이체 실행 요청 DTO
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class TransferRequest extends SecureRequest {

    @NotBlank(message = "출금 은행 코드는 필수입니다.")
    private String withdrawalBankCode;

    @NotBlank(message = "입금 은행 코드는 필수입니다.")
    private String depositBankCode;

    @NotNull(message = "이체 금액은 필수입니다.")
    @Positive(message = "이체 금액은 0보다 커야 합니다.")
    private BigDecimal amount;

    @Override
    @NotBlank(message = "출금 은행 암호화 페이로드는 필수입니다.")
    public String getReqPayload() {
        return super.getReqPayload();
    }

}

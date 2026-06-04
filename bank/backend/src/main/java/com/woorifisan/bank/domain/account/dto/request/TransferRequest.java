package com.woorifisan.bank.domain.account.dto.request;

import com.woorifisan.bank.global.security.dto.SecureRequest;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * 이체 출금 실행 요청 DTO (E2EE 적용)
 */
@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class TransferRequest extends SecureRequest {

    @NotBlank(message = "출금 은행 코드는 필수입니다.")
    private String withdrawalBankCode;

    @NotBlank(message = "입금 은행 코드는 필수입니다.")
    private String depositBankCode;

    @NotNull(message = "이체 금액은 필수입니다.")
    @Min(value = 1, message = "이체 금액은 1원 이상이어야 합니다.")
    private BigDecimal amount;

}

package com.woorifisan.bank.domain.account.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 입금 이체 요청 DTO (타행 -> 당행)
 */
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class DepositRequest {

    @NotBlank(message = "입금 계좌 번호는 필수입니다.")
    private String depositAccountNo;

    @NotNull(message = "입금 금액은 필수입니다.")
    @Min(value = 1, message = "입금 금액은 1원 이상이어야 합니다.")
    private BigDecimal amount;

    @NotBlank(message = "출금 은행 코드는 필수입니다.")
    private String withdrawalBankCode;

    @NotBlank(message = "출금 계좌 번호는 필수입니다.")
    private String withdrawalAccountNo;

}

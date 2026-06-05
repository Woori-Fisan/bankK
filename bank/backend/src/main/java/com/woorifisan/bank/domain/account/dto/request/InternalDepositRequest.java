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
 * 은행 간 통신용 내부 입금 요청 DTO
 */
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class InternalDepositRequest {

    @NotBlank(message = "입금 계좌번호는 필수입니다.")
    private String depositAccountNo;

    @NotNull(message = "입금 금액은 필수입니다.")
    @Min(value = 1, message = "입금 금액은 1원 이상이어야 합니다.")
    private BigDecimal amount;

    @NotBlank(message = "출금 은행 코드는 필수입니다.")
    private String withdrawalBankCode;

    private String withdrawalAccountNo;

}

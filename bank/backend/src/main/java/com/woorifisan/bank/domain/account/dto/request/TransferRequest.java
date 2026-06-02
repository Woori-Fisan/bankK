package com.woorifisan.bank.domain.account.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 이체 실행 요청 DTO
 */
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class TransferRequest {

    @NotBlank(message = "출금 계좌 번호는 필수입니다.")
    private String withdrawalAccountNo;

    @NotBlank(message = "출금 계좌 비밀번호는 필수입니다.")
    private String withdrawalPassword;

    @NotBlank(message = "고객 주민번호 앞자리는 필수입니다.")
    @Pattern(regexp = "\\d{7}", message = "주민번호 앞자리는 숫자 7자리여야 합니다.")
    private String customerRrnPrefix;

    @NotBlank(message = "입금 은행 코드는 필수입니다.")
    private String depositBankCode;

    @NotBlank(message = "입금 계좌 번호는 필수입니다.")
    private String depositAccountNo;

    @NotNull(message = "이체 금액은 필수입니다.")
    @Min(value = 1, message = "이체 금액은 1원 이상이어야 합니다.")
    private BigDecimal amount;

}

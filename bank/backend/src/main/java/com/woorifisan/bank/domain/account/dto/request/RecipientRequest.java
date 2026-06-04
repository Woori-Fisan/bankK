package com.woorifisan.bank.domain.account.dto.request;

import com.woorifisan.bank.global.security.dto.SecureRequest;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * 수취인 확인 요청 DTO
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class RecipientRequest extends SecureRequest {

    @NotBlank(message = "입금 은행 코드는 필수입니다.")
    private String depositBankCode;

}

package com.woorifisan.platform.domain.bank.dto.request;

import com.woorifisan.platform.global.security.dto.SecureRequest;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * 수취인 조회 요청 DTO
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class TransferRecipientRequest extends SecureRequest {

    @NotBlank(message = "입금 은행 코드는 필수입니다.")
    private String depositBankCode;

}

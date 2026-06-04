package com.woorifisan.platform.domain.bank.dto.request;

import com.woorifisan.platform.global.security.dto.SecureRequest;
import jakarta.validation.constraints.NotBlank;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class BalanceInquiryRequest extends SecureRequest {

    @NotBlank(message = "은행 코드는 필수입니다.")
    private String bankCode;

}

package com.woorifisan.platform.domain.bank.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BankDto {

    private String bankCode;
    private String bankName;
}

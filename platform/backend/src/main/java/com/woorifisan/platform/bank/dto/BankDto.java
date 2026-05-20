package com.woorifisan.platform.bank.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BankDto {

    private String bankCode;
    private String bankName;
}

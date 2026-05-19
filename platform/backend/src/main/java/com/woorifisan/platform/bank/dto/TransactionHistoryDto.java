package com.woorifisan.platform.bank.dto;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class TransactionHistoryDto {

    private String txId;
    private String txDate;
    private String txType;
    private Integer amount;
    private Integer balance;
    private String counterpartName;
    private String description;

}

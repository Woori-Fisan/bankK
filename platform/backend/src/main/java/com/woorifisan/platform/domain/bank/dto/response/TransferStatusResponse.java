package com.woorifisan.platform.domain.bank.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class TransferStatusResponse {
    private String transactionId;
    private String status;
    private String message;
}
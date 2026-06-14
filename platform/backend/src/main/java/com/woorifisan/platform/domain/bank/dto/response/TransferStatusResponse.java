package com.woorifisan.platform.domain.bank.dto.response;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class TransferStatusResponse {
    private String txId;
    private String status;
    private String message;
}
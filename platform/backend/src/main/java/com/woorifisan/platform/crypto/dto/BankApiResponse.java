package com.woorifisan.platform.crypto.dto;

import lombok.AllArgsConstructor;
import lombok.Setter;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class BankApiResponse {
    private String bankCode;
    private String keyId;
    private String publicKey;
}

package com.woorifisan.platform.crypto.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PublicKeyResponse {
    private String bankCode;
    private String publicKey;
}
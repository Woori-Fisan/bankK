package com.woorifisan.platform.crypto.controller;

import com.woorifisan.platform.global.response.ApiResponse;
import com.woorifisan.platform.crypto.dto.PublicKeyResponse;
import com.woorifisan.platform.crypto.service.CryptoService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/api/v1/crypto")
public class CryptoController {

    private final CryptoService cryptoService;

    public CryptoController(CryptoService cryptoService) {
        this.cryptoService = cryptoService;
    }

    @GetMapping("/public-key")
    public ApiResponse<PublicKeyResponse> getPublicKey(@RequestParam String bankCode) {
        PublicKeyResponse response = cryptoService.getPublicKey(bankCode);
        return ApiResponse.success(response);
    }
}
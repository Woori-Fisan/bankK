package com.woorifisan.bank.domain.key.controller;

import com.woorifisan.bank.domain.key.dto.request.BankRsaKeyRegisterRequest;
import com.woorifisan.bank.domain.key.dto.response.BankRsaKeyResponse;
import com.woorifisan.bank.domain.key.service.BankRsaKeyService;
import com.woorifisan.bank.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * RSA 키 관련 API 컨트롤러
 */
@Tag(name = "암호화 키 (Key)", description = "RSA 공개키 조회 관련 API")
@RestController
@RequestMapping("/api/v1/baas/keys")
@RequiredArgsConstructor
public class BankRsaKeyController {

    private final BankRsaKeyService bankRsaKeyService;

    @Operation(summary = "최신 RSA 공개키 조회", description = "종단간 암호화를 위한 은행의 최신 RSA 공개키와 키 ID를 조회합니다.")
    @GetMapping("/public")
    public ApiResponse<BankRsaKeyResponse> getLatestPublicKey() {
        return ApiResponse.success(bankRsaKeyService.getLatestPublicKey());
    }

    @Operation(summary = "RSA 키 등록", description = "새로운 RSA 키 쌍을 등록합니다. 키 ID는 자동으로 생성됩니다.")
    @PostMapping
    public ApiResponse<BankRsaKeyResponse> registerKey(@Valid @RequestBody BankRsaKeyRegisterRequest request) {
        return ApiResponse.success(bankRsaKeyService.registerKey(request));
    }
}

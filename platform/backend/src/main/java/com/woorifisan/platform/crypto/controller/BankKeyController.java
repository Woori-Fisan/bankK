package com.woorifisan.platform.crypto.controller;

import com.woorifisan.platform.crypto.dto.response.BankRsaKeyResponse;
import com.woorifisan.platform.crypto.service.BankKeyService;
import com.woorifisan.platform.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 은행 암호화 키 관련 API 컨트롤러
 */
@Tag(name = "은행 암호화 키 (Bank Key)", description = "종단간 암호화(E2EE)를 위한 은행 공개키 조회 API")
@RestController
@RequestMapping("/api/v1/keys")
@RequiredArgsConstructor
public class BankKeyController {

    private final BankKeyService bankKeyService;

    @Operation(summary = "모든 은행 RSA 공개키 조회", description = "플랫폼에 등록된 모든 은행의 최신 RSA 공개키와 키 ID를 조회합니다.")
    @GetMapping("/public")
    public ApiResponse<Map<String, BankRsaKeyResponse>> getAllPublicKeys() {
        return ApiResponse.success(bankKeyService.getAllLatestPublicKeys());
    }
}

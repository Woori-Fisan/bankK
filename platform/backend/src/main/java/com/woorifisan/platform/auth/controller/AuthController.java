package com.woorifisan.platform.auth.controller;

import com.woorifisan.platform.auth.dto.LoginRequest;
import com.woorifisan.platform.auth.dto.LoginResponse;
import com.woorifisan.platform.auth.dto.TokenRefreshRequest;
import com.woorifisan.platform.auth.dto.TokenRefreshResponse;
import com.woorifisan.platform.auth.service.AuthService;
import com.woorifisan.platform.global.config.swagger.CustomExceptionDescription;
import com.woorifisan.platform.global.config.swagger.SwaggerResponseDescription;
import com.woorifisan.platform.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    // 로그인
    // POST /api/v1/auth/login
    @PostMapping("/login")
    @CustomExceptionDescription(SwaggerResponseDescription.AUTH_LOGIN)
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request
    ) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // 로그아웃
    // POST /api/v1/auth/logout
    // staffId는 JwtAuthFilter에서 SecurityContext에 저장된 값 사용
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @org.springframework.security.core.annotation.AuthenticationPrincipal Long staffId
    ) {
        authService.logout(staffId);
        return ResponseEntity.ok(ApiResponse.success());
    }

    // Access Token 재발급 (RTR)
    // POST /api/v1/auth/refresh
    @PostMapping("/refresh")
    @CustomExceptionDescription(SwaggerResponseDescription.AUTH_REFRESH)
    public ResponseEntity<ApiResponse<TokenRefreshResponse>> refresh(
            @Valid @RequestBody TokenRefreshRequest request
    ) {
        TokenRefreshResponse response = authService.refresh(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
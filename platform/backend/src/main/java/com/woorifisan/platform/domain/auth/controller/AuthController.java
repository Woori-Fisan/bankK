package com.woorifisan.platform.domain.auth.controller;

import com.woorifisan.platform.domain.auth.dto.AuthTokenDto;
import com.woorifisan.platform.domain.auth.dto.request.LoginRequest;
import com.woorifisan.platform.domain.auth.dto.response.LoginResponse;
import com.woorifisan.platform.domain.auth.dto.response.PublicAuthKeyResponse;
import com.woorifisan.platform.domain.auth.dto.response.TokenRefreshResponse;
import com.woorifisan.platform.domain.auth.service.AuthService;
import com.woorifisan.platform.global.config.resolver.CurrentUser;
import com.woorifisan.platform.global.config.swagger.CustomExceptionDescription;
import com.woorifisan.platform.global.config.swagger.SwaggerResponseDescription;
import com.woorifisan.platform.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import com.woorifisan.platform.global.aop.annotation.ExcludeLogging;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@ExcludeLogging
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    // 플랫폼 암호화용 공개키 제공
    // GET /api/v1/auth/public-key
    @GetMapping("/public-key")
    public ResponseEntity<ApiResponse<PublicAuthKeyResponse>> getPublicKey() {
        PublicAuthKeyResponse response = authService.getPublicKey();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // 로그인
    // POST /api/v1/auth/login
    @PostMapping("/login")
    @CustomExceptionDescription(SwaggerResponseDescription.AUTH_LOGIN)
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request,
            @RequestHeader(value = "x-jws-signature", required = true) String jwsSignature,
            HttpServletResponse response
    ) {
        AuthTokenDto tokenDto = authService.login(request, jwsSignature);

        // Refresh Token을 HttpOnly 쿠키로 설정
        ResponseCookie cookie = ResponseCookie.from("refreshToken", tokenDto.getRefreshToken())
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path("/")
                .maxAge(tokenDto.getRefreshTokenExpiresIn())
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        LoginResponse loginResponse = LoginResponse.builder()
                .accessToken(tokenDto.getAccessToken())
                .role(tokenDto.getRole())
                .refreshTokenExpiresIn(tokenDto.getRefreshTokenExpiresIn())
                .build();

        return ResponseEntity.ok(ApiResponse.success(loginResponse));
    }

    // 로그아웃
    // POST /api/v1/auth/logout
    // staffId는 JwtAuthFilter에서 SecurityContext에 저장된 값 사용
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @Parameter(hidden = true) @CurrentUser Long staffId,
            HttpServletResponse response
    ) {
        authService.logout(staffId);

        // 쿠키 삭제
        ResponseCookie cookie = ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path("/")
                .maxAge(0)
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        return ResponseEntity.ok(ApiResponse.success());
    }

    // Access Token 재발급 (RTR)
    // POST /api/v1/auth/refresh
    @PostMapping("/refresh")
    @CustomExceptionDescription(SwaggerResponseDescription.AUTH_REFRESH)
    public ResponseEntity<ApiResponse<TokenRefreshResponse>> refresh(
            @CookieValue(value = "refreshToken", required = true) String refreshToken,
            HttpServletResponse response
    ) {
        AuthTokenDto tokenDto = authService.refresh(refreshToken);

        // 새로운 Refresh Token을 HttpOnly 쿠키로 설정 (RTR)
        ResponseCookie cookie = ResponseCookie.from("refreshToken", tokenDto.getRefreshToken())
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path("/")
                .maxAge(tokenDto.getRefreshTokenExpiresIn())
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        TokenRefreshResponse refreshResponse = TokenRefreshResponse.builder()
                .accessToken(tokenDto.getAccessToken())
                .accessTokenExpiresIn(tokenDto.getAccessTokenExpiresIn())
                .refreshTokenExpiresIn(tokenDto.getRefreshTokenExpiresIn())
                .build();

        return ResponseEntity.ok(ApiResponse.success(refreshResponse));
    }
}
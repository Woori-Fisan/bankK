package com.woorifisan.monitoring.domain.user.controller;

import com.woorifisan.monitoring.domain.user.dto.AuthTokenDto;
import com.woorifisan.monitoring.domain.user.dto.request.LoginRequest;
import com.woorifisan.monitoring.domain.user.dto.request.RegisterRequest;
import com.woorifisan.monitoring.domain.user.dto.response.LoginResponse;
import com.woorifisan.monitoring.domain.user.dto.response.TokenRefreshResponse;
import com.woorifisan.monitoring.domain.user.service.UserService;
import com.woorifisan.monitoring.global.config.swagger.CustomExceptionDescription;
import com.woorifisan.monitoring.global.config.swagger.SwaggerResponseDescription;
import com.woorifisan.monitoring.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@Tag(name = "User", description = "운영 관리자 계정 관리 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/user")
public class UserController {

    private final UserService userService;

    @Operation(summary = "로그인", description = "관리자 아이디와 비밀번호로 로그인을 수행합니다.")
    @CustomExceptionDescription(SwaggerResponseDescription.USER_LOGIN)
    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(
            @Valid @RequestBody LoginRequest request,
            @Parameter(hidden = true) HttpServletResponse response
    ) {
        AuthTokenDto tokenDto = userService.login(request);

        // Refresh Token을 HttpOnly 쿠키로 설정
        ResponseCookie cookie = ResponseCookie.from("refreshToken", tokenDto.getRefreshToken())
                .httpOnly(true)
                .secure(true) // HTTPS 환경 권장
                .sameSite("Strict")
                .path("/")
                .maxAge(tokenDto.getRefreshTokenExpiresIn())
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        LoginResponse loginResponse = LoginResponse.builder()
                .accessToken(tokenDto.getAccessToken())
                .loginId(tokenDto.getLoginId())
                .build();

        return ApiResponse.success(loginResponse);
    }

    @Operation(summary = "로그아웃", description = "현재 세션을 종료하고 Refresh Token 쿠키 및 Redis 세션을 삭제합니다.")
    @PostMapping("/logout")
    public ApiResponse<Void> logout(
            @Parameter(hidden = true) @AuthenticationPrincipal Long userId,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader,
            @Parameter(hidden = true) HttpServletResponse response
    ) {
        String accessToken = null;
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            accessToken = authHeader.substring(7);
        }

        if (userId != null) {
            userService.logout(userId, accessToken);
        }

        // 쿠키 삭제
        ResponseCookie cookie = ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path("/")
                .maxAge(0)
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        return ApiResponse.success();
    }

    @Operation(summary = "계정 등록", description = "새로운 운영 관리자 계정을 등록합니다.")
    @CustomExceptionDescription(SwaggerResponseDescription.USER_REGISTER)
    @PostMapping("/register")
    public ApiResponse<Void> register(@Valid @RequestBody RegisterRequest request) {
        userService.register(request);
        return ApiResponse.success();
    }

    @Operation(summary = "토큰 재발급", description = "Refresh Token 쿠키를 사용하여 새로운 Access Token과 Refresh Token(RTR)을 발급합니다.")
    @CustomExceptionDescription(SwaggerResponseDescription.AUTH_REFRESH)
    @PostMapping("/refresh")
    public ApiResponse<TokenRefreshResponse> refresh(
            @CookieValue(value = "refreshToken") String refreshToken,
            @Parameter(hidden = true) HttpServletResponse response
    ) {
        AuthTokenDto tokenDto = userService.refresh(refreshToken);

        // 새로운 Refresh Token을 HttpOnly 쿠키로 설정 (RTR 적용)
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
                .accessTokenExpiresIn((int) tokenDto.getAccessTokenExpiresIn())
                .build();

        return ApiResponse.success(refreshResponse);
    }
}

package com.woorifisan.platform.domain.auth.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AuthTokenDto {
    private String accessToken;
    private String refreshToken;
    private String role;
    private long accessTokenExpiresIn;
    private long refreshTokenExpiresIn;
}

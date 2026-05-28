package com.woorifisan.monitoring.domain.user.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AuthTokenDto {
    private String accessToken;
    private String refreshToken;
    private long refreshTokenExpiresIn;
    private String loginId;
}

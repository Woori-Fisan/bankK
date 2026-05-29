package com.woorifisan.monitoring.domain.user.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AuthTokenDto {
    private String accessToken;
    private long   accessTokenExpiresIn; // 초 단위
    private String refreshToken;
    private long   refreshTokenExpiresIn; // 초 단위
    private String loginId;
}

package com.woorifisan.platform.domain.auth.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TokenRefreshResponse {

    private String accessToken;
    private String refreshToken;
    private int accessTokenExpiresIn;   // 초 단위 (900 = 15분)
    private int refreshTokenExpiresIn;  // 초 단위 (28800 = 8시간)
}
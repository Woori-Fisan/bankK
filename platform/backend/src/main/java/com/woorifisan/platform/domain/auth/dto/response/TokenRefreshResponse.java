package com.woorifisan.platform.domain.auth.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TokenRefreshResponse {

    private String accessToken;
    private int accessTokenExpiresIn;   // 초 단위 (900 = 15분)
}
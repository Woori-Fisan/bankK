package com.woorifisan.platform.domain.auth.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LoginResponse {

    private String accessToken;
    private String role;
    private long refreshTokenExpiresIn;
}
package com.woorifisan.platform.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class TokenRefreshRequest {

    @NotBlank(message = "Refresh Token을 입력해주세요.")
    private String refreshToken;
}
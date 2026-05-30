package com.woorifisan.monitoring.domain.user.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "토큰 재발급 응답")
public class TokenRefreshResponse {
    @Schema(description = "새로운 Access Token")
    private String accessToken;

    @Schema(description = "Access Token 만료 시간 (초)")
    private int accessTokenExpiresIn;
}

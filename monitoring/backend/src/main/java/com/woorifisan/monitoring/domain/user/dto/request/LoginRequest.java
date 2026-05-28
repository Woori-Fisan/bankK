package com.woorifisan.monitoring.domain.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class LoginRequest {
    @Schema(description = "관리자 아이디", example = "admin")
    @NotBlank(message = "아이디를 입력해주세요.")
    private String loginId;

    @Schema(description = "비밀번호", example = "password")
    @NotBlank(message = "비밀번호를 입력해주세요.")
    private String password;
}

package com.woorifisan.platform.domain.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class LoginRequest {

    @NotBlank(message = "사번을 입력해주세요.")
    private String employeeId;

    @NotBlank(message = "비밀번호를 입력해주세요.")
    private String password;
}
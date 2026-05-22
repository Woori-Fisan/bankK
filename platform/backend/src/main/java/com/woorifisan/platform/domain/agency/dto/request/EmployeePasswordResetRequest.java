package com.woorifisan.platform.domain.agency.dto.request;

import jakarta.validation.constraints.NotBlank;

public record EmployeePasswordResetRequest(
        @NotBlank(message = "비밀번호는 필수입니다.")
        String password
) {
}
package com.woorifisan.platform.domain.agency.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;


public record EmployeeRegisterRequest(
        @NotBlank(message = "ID는 필수입니다.")
        String loginId,
        
        @NotBlank(message = "비밀번호는 필수입니다.")
        String password,
        
        @NotBlank(message = "권한은 필수입니다.")
        String role,
        
        @NotNull(message = "소속 대행업체 ID는 필수입니다.")
        Long agencyId,
        
        @NotBlank(message = "대행업체 사번은 필수입니다.")
        String employeeNum
) {
}
package com.woorifisan.platform.domain.agency.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeRegisterRequest {
    @NotBlank(message = "ID는 필수입니다.")
    private String loginId;
    
    @NotBlank(message = "비밀번호는 필수입니다.")
    private String password;
    
    @NotBlank(message = "권한은 필수입니다.")
    private String role;
    
    @NotNull(message = "소속 대행업체 ID는 필수입니다.")
    private Long agencyId;
    
    @NotBlank(message = "대행업체 사번은 필수입니다.")
    private String employeeNum;
}

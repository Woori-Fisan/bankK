package com.woorifisan.platform.domain.agency.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeDto {
    private String loginId;
    private String employeeNum;
    private Long agencyId;
    private String role;
    private Boolean isLocked;
    private Boolean isDeleted;
    private LocalDateTime createdAt;
}

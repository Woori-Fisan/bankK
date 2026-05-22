package com.woorifisan.platform.domain.agency.dto;

import java.time.LocalDateTime;

public record EmployeeDto(
        String loginId,
        String employeeNum,
        Long agencyId,
        String role,
        Boolean isLocked,
        Boolean isDeleted,
        LocalDateTime createdAt
) {
}
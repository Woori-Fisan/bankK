package com.woorifisan.platform.domain.agency.dto.response;

import java.time.LocalDateTime;

public record EmployeePasswordResetResponse(
        String loginId,
        Boolean isLocked,
        LocalDateTime updatedAt
) {
}
package com.woorifisan.platform.domain.agency.dto.response;

import java.time.LocalDateTime;

public record EmployeeDeleteResponse(
        String loginId,
        Boolean isDeleted,
        LocalDateTime updatedAt
) {
}
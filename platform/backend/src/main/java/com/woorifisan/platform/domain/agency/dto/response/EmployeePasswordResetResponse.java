package com.woorifisan.platform.domain.agency.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EmployeePasswordResetResponse {
    private String loginId;
    private Boolean isLocked;
    private LocalDateTime updatedAt;
}

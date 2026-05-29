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
public class EmployeeDeleteResponse {
    private String loginId;
    private Boolean isDeleted;
    private LocalDateTime updatedAt;
}

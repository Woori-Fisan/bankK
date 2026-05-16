package com.woorifisan.platform.auth.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlatformUser {

    private Long id;
    private Long agencyId;
    private String loginId;
    private String passwordHash;
    private String role;
    private int failedLoginCount;
    private boolean isLocked;
    private boolean isDeleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
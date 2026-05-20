package com.woorifisan.platform.domain.user.model;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 플랫폼 운영자 정보 모델
 */
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class PlatformAdmin {

    private Long id;
    private String loginId;
    private String passwordHash;
    private String name;
    private String role;
    private boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * PlatformAdmin 생성을 위한 정적 팩토리 메서드
     */
    public static PlatformAdmin of(String loginId, String passwordHash, String name, String role) {
        return PlatformAdmin.builder()
                .loginId(loginId)
                .passwordHash(passwordHash)
                .name(name)
                .role(role)
                .isActive(true)
                .build();
    }
}

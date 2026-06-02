package com.woorifisan.platform.domain.agency.model;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 대행업체 직원(Agency User) 정보 모델
 */
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class AgencyUser {

    private Long id;
    private String loginId;
    private String passwordHash;
    private String role;
    private Long agencyId;
    private String employeeNum;
    private Boolean isLocked;
    private Boolean isDeleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * AgencyUser 생성을 위한 정적 팩토리 메서드
     */
    public static AgencyUser of(String loginId, String passwordHash, String role, Long agencyId, String employeeNum) {
        LocalDateTime now = LocalDateTime.now();
        return AgencyUser.builder()
                .loginId(loginId)
                .passwordHash(passwordHash)
                .role(role)
                .agencyId(agencyId)
                .employeeNum(employeeNum)
                .isLocked(false)
                .isDeleted(false)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }
}

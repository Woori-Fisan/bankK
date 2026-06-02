package com.woorifisan.platform.domain.agency.model;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 대행기관 정보 모델
 */
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Agency {

    private Long id;
    private String agencyCode;
    private String agencyName;
    private String allowedServices;
    private boolean isActive;
    private LocalDate contractStart;
    private LocalDate contractEnd;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

}

package com.woorifisan.monitoring.domain.log.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 거래 로그 목록 조회용 DTO (필수 정보만 포함하여 경량화)
 */
@Getter
@Setter
public class LogListDTO {
    private String logId;
    private LocalDateTime createdAt;
    private String level;
    private String logType;
    private String traceId;
    private String staffId;
    private String bankCode;
    private String targetCode;
    private String agencyCode;
    private String httpMethod;
    private String httpUri;
    private Integer httpStatus;
    private Long elapsedMs;
    private String errorCode;
}

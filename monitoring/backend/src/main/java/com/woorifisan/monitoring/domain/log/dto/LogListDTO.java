package com.woorifisan.monitoring.domain.log.dto;

import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class LogListDTO {
    private Long id;
    private LocalDateTime createdAt;
    private String level;
    private String logType;
    private String traceId;
    private String staffId;
    private String bankCode;
    private String targetCode;
    private String agencyCode;
    private String bankKeyId;
    private String httpMethod;
    private String httpUri;
    private Integer httpStatus;
    private Long elapsedMs;
    private String clientIp;
    private String jwsSignature;
    private String bodyData;
    private String errorCode;
    private String errorMessage;
}

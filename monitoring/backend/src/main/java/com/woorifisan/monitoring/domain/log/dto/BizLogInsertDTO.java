package com.woorifisan.monitoring.domain.log.dto;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BizLogInsertDTO {
    private LocalDateTime timestamp;
    private String level;
    private String logType;
    private String traceId;
    private String staffId;
    private String agencyCode;
    private String bankCode;
    private String targetCode;
    private String bankKeyId;
    private String httpMethod;
    private String httpUri;
    private Integer httpStatus;
    private Long elapsedMs;
    private String clientIp;
    private String bodyData;
    private String errorCode;
    private String errorMessage;
}

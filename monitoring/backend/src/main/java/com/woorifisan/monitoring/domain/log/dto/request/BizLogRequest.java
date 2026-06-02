package com.woorifisan.monitoring.domain.log.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.woorifisan.monitoring.domain.log.dto.BizLogInsertDTO;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class BizLogRequest {

    @JsonProperty("@timestamp")
    private String timestamp;

    private String level;
    private String traceId;
    private String staffId;

    private HttpContext http;

    @Getter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class HttpContext {

        private String logType;
        private String bankCode;
        private String targetCode;
        private String bankKeyId;
        private String clientIp;
        private Long elapsedMs;
        private String errorCode;
        private String errorMessage;
        private String jwsSignature;
        private String httpMethod;
        private String httpUri;
        private Integer httpStatus;
        private String request;
        private String response;

    }

    public BizLogInsertDTO toInsertDTO() {
        return BizLogInsertDTO.builder()
                .timestamp(parseTimestamp(timestamp))
                .level(level)
                .logType(http.getLogType())
                .traceId(traceId)
                .staffId(staffId)
                .bankCode(http.getBankCode())
                .targetCode(http.getTargetCode())
                .bankKeyId(http.getBankKeyId())
                .httpMethod(http.getHttpMethod())
                .httpUri(http.getHttpUri())
                .httpStatus(http.getHttpStatus())
                .elapsedMs(http.getElapsedMs())
                .clientIp(http.getClientIp())
                .jwsSignature(http.getJwsSignature())
                .bodyData(resolveBodyData())
                .errorCode(http.getErrorCode())
                .errorMessage(http.getErrorMessage())
                .build();
    }

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private LocalDateTime parseTimestamp(String ts) {
        if (ts == null) return LocalDateTime.now(KST);
        try {
            return LocalDateTime.ofInstant(Instant.parse(ts), KST);
        } catch (Exception e) {
            return LocalDateTime.now(KST);
        }
    }

    // ERR 로그는 null, RES 로그는 response, REQ 로그는 request를 body_data로 저장
    private String resolveBodyData() {
        if (http == null) return null;
        String logType = http.logType;
        if (logType == null) return null;
        if (logType.endsWith("_ERR")) return null;
        if (logType.endsWith("_RES")) return http.response;
        return http.request;
    }
}

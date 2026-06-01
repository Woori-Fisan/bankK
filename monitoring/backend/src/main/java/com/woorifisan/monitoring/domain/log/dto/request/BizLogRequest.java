package com.woorifisan.monitoring.domain.log.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
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

    // RES 로그는 response, 나머지는 request를 body_data로 저장
    public String resolveBodyData() {
        if (http == null) return null;
        String logType = http.logType;
        if (logType != null && logType.endsWith("_RES")) {
            return http.response;
        }
        return http.request;
    }
}

package com.woorifisan.platform.global.logging.model;

import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * system_logs 테이블 매핑 모델.
 *
 * <p>AOP Aspect에서 출력한 구조화 로그를 DB에 저장하기 위한 단순 모델 클래스.
 * JPA를 사용하지 않으며, MyBatis {@link com.woorifisan.platform.global.logging.mapper.SystemLogMapper}를 통해 삽입된다.</p>
 */
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class SystemLog {

    private Long          id;
    private LocalDateTime createdAt;
    private String        level;         // INFO / WARN / ERROR
    private String        logType;       // CONTROLLER_REQ, CONTROLLER_RES, CONTROLLER_ERR, BANK_REQ, BANK_RES, BANK_ERR, BANK_COMM_ERR

    private String        traceId;
    private String        staffId;
    private String        agencyCode;

    private String        bankCode;      // NOT NULL — 없을 경우 "UNKNOWN"
    private String        targetCode;
    private String        bankKeyId;

    private String        httpMethod;
    private String        httpUri;
    private Integer       httpStatus;
    private Integer       elapsedMs;
    private String        clientIp;

    private String        jwsSignature;
    private String        bodyData;

    private String        errorCode;
    private String        errorMessage;
}

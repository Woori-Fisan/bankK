package com.woorifisan.monitoring.domain.log.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * 거래 로그 단건 상세 조회용 DTO (페이로드 등 모든 정보 포함)
 */
@Getter
@Setter
public class LogDetailDTO extends LogListDTO {
    private String bankKeyId;
    private String clientIp;
    private String bodyData;
    private String errorMessage;
}

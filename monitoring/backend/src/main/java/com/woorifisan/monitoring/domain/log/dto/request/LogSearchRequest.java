package com.woorifisan.monitoring.domain.log.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LogSearchRequest {
    @Schema(description = "로그 레벨 (INFO, WARN, ERROR)", example = "INFO")
    private String level;

    @Schema(description = "로그 유형 (CONTROLLER_REQ, BANK_REQ 등)", example = "CONTROLLER_REQ")
    private String logType;

    @Schema(description = "HTTP 상태 코드", example = "200")
    private String httpStatus;

    @Schema(description = "에러 코드", example = "B001")
    private String errorCode;

    @Schema(description = "대행기관 코드", example = "PO001")
    private String agencyCode;

    @Schema(description = "대행업자(사용자) ID", example = "admin")
    private String staffId;

    @Schema(description = "은행 코드", example = "020")
    private String bankCode;

    @Schema(description = "트레이스 ID (전체 흐름 추적)", example = "t2-transfer-002")
    private String traceId;

    @Schema(description = "조회 시작일 (yyyy-MM-dd HH:mm:ss)", example = "2026-05-29 00:00:00")
    @NotBlank(message = "조회 시작일은 필수입니다.")
    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}$", message = "날짜 형식은 yyyy-MM-dd HH:mm:ss 이어야 합니다.")
    private String startDate;

    @Schema(description = "조회 종료일 (yyyy-MM-dd HH:mm:ss)", example = "2026-05-29 23:59:59")
    @NotBlank(message = "조회 종료일은 필수입니다.")
    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}$", message = "날짜 형식은 yyyy-MM-dd HH:mm:ss 이어야 합니다.")
    private String endDate;
}

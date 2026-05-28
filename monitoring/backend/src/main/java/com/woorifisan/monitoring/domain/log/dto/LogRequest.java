package com.woorifisan.monitoring.domain.log.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LogRequest {
    @Schema(description = "대행기관 코드", example = "POST_001")
    private String agencyCode;

    @Schema(description = "은행 코드", example = "BANK_001")
    private String bankCode;

    @Schema(description = "직원 ID", example = "EMP_12345")
    private String staffId;

    @Schema(description = "조회 시작일 (yyyy-MM-dd HH:mm:ss)", example = "2026-05-01 00:00:00")
    @NotBlank(message = "조회 시작일은 필수입니다.")
    private String startDate;

    @Schema(description = "조회 종료일 (yyyy-MM-dd HH:mm:ss)", example = "2026-05-31 23:59:59")
    @NotBlank(message = "조회 종료일은 필수입니다.")
    private String endDate;

    @Schema(description = "로그 유형", example = "TRANS_LOG")
    private String logType;

    @Schema(description = "HTTP 상태 코드", example = "200")
    private String httpStatus; // 쿼리 파라미터 매핑을 위해 String 유지 또는 타입 확인 필요

    // Pagination
    @Schema(description = "페이지 번호 (0부터 시작)", example = "0")
    @Min(value = 0, message = "페이지 번호는 0 이상이어야 합니다.")
    private int page = 0;

    @Schema(description = "페이지 크기", example = "20")
    @Min(value = 1, message = "페이지 크기는 1 이상이어야 합니다.")
    private int size = 20;

    public int getOffset() {
        return Math.max(0, page * size);
    }
}

package com.woorifisan.monitoring.domain.log.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LogSummaryResponse {
    @Schema(description = "총 로그 수", example = "1284")
    private long totalCount;

    @Schema(description = "오류 건수", example = "42")
    private long errorCount;

    @Schema(description = "평균 응답시간 (ms)", example = "350.5")
    private double averageElapsedMs;

    @Schema(description = "처리 성공률 (%)", example = "96.7")
    private double successRate;
}

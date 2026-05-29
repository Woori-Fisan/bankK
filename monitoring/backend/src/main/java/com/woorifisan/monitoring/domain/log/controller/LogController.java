package com.woorifisan.monitoring.domain.log.controller;

import com.woorifisan.monitoring.domain.log.dto.LogListDTO;
import com.woorifisan.monitoring.domain.log.dto.request.LogRequest;
import com.woorifisan.monitoring.domain.log.dto.response.LogResponse;
import com.woorifisan.monitoring.domain.log.dto.request.LogSummaryRequest;
import com.woorifisan.monitoring.domain.log.dto.response.LogSummaryResponse;
import com.woorifisan.monitoring.domain.log.service.LogService;
import com.woorifisan.monitoring.global.config.swagger.CustomExceptionDescription;
import com.woorifisan.monitoring.global.config.swagger.SwaggerResponseDescription;
import com.woorifisan.monitoring.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.*;

@Slf4j
@Tag(name = "Log", description = "거래 로그 조회 및 모니터링 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/monitor")
public class LogController {

    private final LogService logService;

    @Operation(summary = "거래 로그 조회", description = "Trace ID, 기간, 상태 코드 등을 조건으로 거래 로그 목록을 조회합니다.")
    @CustomExceptionDescription(SwaggerResponseDescription.LOG_INQUIRY)
    @GetMapping("/transactions")
    public ApiResponse<LogResponse> getLogs(@Valid @ParameterObject @ModelAttribute LogRequest request) {
        log.info("[API 요청] 거래 로그 조회 - 시작일: {}, 종료일: {}", request.getStartDate(), request.getEndDate());

        LogResponse response = logService.getLogList(request);

        log.info("[API 응답] 거래 로그 조회 완료 - 총 페이지: {}, 조회된 로그 개수: {}", 
                response.getTotalPage(), response.getLogListDTO().size());

        return ApiResponse.success(response);
    }

    @Operation(summary = "거래 로그 단건 조회", description = "로그 고유 ID를 통해 특정 거래의 상세 페이로드, 서명, 에러 메시지 등 모든 정보를 조회합니다.")
    @GetMapping("/transactions/{id}")
    public ApiResponse<LogListDTO> getLog(
            @Parameter(description = "로그 PK ID", example = "1")
            @PathVariable("id") Long id) {
        log.info("[API 요청] 거래 로그 단건 조회 - ID: {}", id);

        LogListDTO response = logService.getLogDetail(id);

        log.info("[API 응답] 거래 로그 단건 조회 완료 - ID: {}, Trace ID: {}", response.getId(), response.getTraceId());

        return ApiResponse.success(response);
    }

    @Operation(summary = "거래 로그 통계 요약", description = "조회 조건에 따른 총 로그 수, 오류 건수, 평균 응답시간, 처리 성공률을 반환합니다.")
    @GetMapping("/summary")
    public ApiResponse<LogSummaryResponse> getSummary(@Valid @ParameterObject @ModelAttribute LogSummaryRequest request) {
        log.info("[API 요청] 거래 로그 통계 요약 조회 - 시작일: {}, 종료일: {}", request.getStartDate(), request.getEndDate());

        LogSummaryResponse response = logService.getLogSummary(request);

        log.info("[API 응답] 거래 로그 통계 요약 조회 완료 - 총 건수: {}, 성공률: {}%", response.getTotalCount(), response.getSuccessRate());

        return ApiResponse.success(response);
    }
}

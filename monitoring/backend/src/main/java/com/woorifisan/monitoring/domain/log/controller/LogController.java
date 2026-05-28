package com.woorifisan.monitoring.domain.log.controller;

import com.woorifisan.monitoring.domain.log.dto.LogRequest;
import com.woorifisan.monitoring.domain.log.dto.LogResponse;
import com.woorifisan.monitoring.domain.log.service.LogService;
import com.woorifisan.monitoring.global.config.swagger.CustomExceptionDescription;
import com.woorifisan.monitoring.global.config.swagger.SwaggerResponseDescription;
import com.woorifisan.monitoring.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}

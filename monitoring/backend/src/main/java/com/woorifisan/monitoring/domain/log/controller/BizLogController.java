package com.woorifisan.monitoring.domain.log.controller;

import com.woorifisan.monitoring.domain.log.dto.request.BizLogRequest;
import com.woorifisan.monitoring.domain.log.service.LogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@Tag(name = "BizLog", description = "Fluent Bit 비즈니스 로그 수신 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/logs")
public class BizLogController {

    private final LogService logService;

    @Operation(summary = "비즈니스 로그 수신", description = "Fluent Bit이 전송하는 비즈니스 로그 배열을 수신하여 DB에 저장합니다.")
    @PostMapping("/biz")
    public ResponseEntity<Void> receiveBizLogs(@RequestBody List<BizLogRequest> logs) {
        log.debug("[BizLog 수신] {} 건", logs.size());
        logService.saveBizLogs(logs);
        return ResponseEntity.ok().build();
    }
}

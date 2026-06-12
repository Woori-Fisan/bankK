package com.woorifisan.monitoring.domain.log.controller;

import com.woorifisan.monitoring.domain.log.dto.request.BizLogRequest;
import com.woorifisan.monitoring.domain.log.service.BizLogBuffer;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "BizLog", description = "Fluent Bit 비즈니스 로그 수신 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/logs")
public class BizLogController {

    private final BizLogBuffer bizLogBuffer;

    @Operation(summary = "비즈니스 로그 수신", description = "Fluent Bit이 전송하는 비즈니스 로그 배열을 수신하여 버퍼에 적재합니다.")
    @PostMapping("/biz")
    public ResponseEntity<Void> receiveBizLogs(@RequestBody List<BizLogRequest> logs) {
        logs.stream()
                .filter(req -> req != null && req.getHttp() != null && req.getHttp().getLogType() != null)
                .map(BizLogRequest::toInsertDTO)
                .forEach(bizLogBuffer::offer);
        return ResponseEntity.ok().build();
    }
}
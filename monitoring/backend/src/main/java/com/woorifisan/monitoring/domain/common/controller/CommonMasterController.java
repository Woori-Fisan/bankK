package com.woorifisan.monitoring.domain.common.controller;

import com.woorifisan.monitoring.domain.common.dto.response.AgencyResponseDTO;
import com.woorifisan.monitoring.domain.common.dto.response.BankResponseDTO;
import com.woorifisan.monitoring.domain.common.service.CommonMasterService;
import com.woorifisan.monitoring.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@Tag(name = "Master Data", description = "시스템 공통 마스터 데이터 조회 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/monitor/common")
public class CommonMasterController {
    private final CommonMasterService commonMasterService;

    @Operation(summary = "은행 목록 조회", description = "시스템에 등록된 모든 활성 은행의 코드와 명칭을 조회합니다.")
    @GetMapping("/banks")
    public ApiResponse<List<BankResponseDTO>> getBanks() {
        log.info("[API 요청] 은행 목록 조회");
        List<BankResponseDTO> response = commonMasterService.getBankList();
        return ApiResponse.success(response);
    }

    @Operation(summary = "대행기관 목록 조회", description = "시스템에 등록된 모든 활성 대행기관의 코드와 명칭을 조회합니다.")
    @GetMapping("/agencies")
    public ApiResponse<List<AgencyResponseDTO>> getAgencies() {
        log.info("[API 요청] 대행기관 목록 조회");
        List<AgencyResponseDTO> response = commonMasterService.getAgencyList();
        return ApiResponse.success(response);
    }
}

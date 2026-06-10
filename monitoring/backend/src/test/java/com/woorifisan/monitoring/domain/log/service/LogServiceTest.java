package com.woorifisan.monitoring.domain.log.service;

import com.woorifisan.monitoring.domain.log.dto.LogDetailDTO;
import com.woorifisan.monitoring.domain.log.dto.LogListDTO;
import com.woorifisan.monitoring.domain.log.dto.request.LogRequest;
import com.woorifisan.monitoring.domain.log.dto.request.LogSummaryRequest;
import com.woorifisan.monitoring.domain.log.dto.response.LogResponse;
import com.woorifisan.monitoring.domain.log.dto.response.LogSummaryResponse;
import com.woorifisan.monitoring.domain.log.mapper.LogMapper;
import com.woorifisan.monitoring.global.exception.BusinessException;
import com.woorifisan.monitoring.global.response.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LogServiceTest {

    @InjectMocks
    private LogService logService;

    @Mock
    private LogMapper logMapper;

    @Test
    @DisplayName("로그목록조회_조건에_맞는_로그목록과_총페이지를_반환한다")
    void getLogList_validRequest_returnsLogListAndTotalPages() {
        // given
        LogRequest request = new LogRequest();
        request.setPage(1);
        request.setSize(10);
        request.setStartDate("2026-06-10 00:00:00");
        request.setEndDate("2026-06-10 23:59:59");

        List<LogListDTO> logs = new ArrayList<>();
        logs.add(new LogListDTO());

        given(logMapper.countLogList(request)).willReturn(25L);
        given(logMapper.findLogList(request)).willReturn(logs);

        // when
        LogResponse response = logService.getLogList(request);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getPageNum()).isEqualTo(1);
        assertThat(response.getPageSize()).isEqualTo(10);
        assertThat(response.getTotalPage()).isEqualTo(3L); // ceil(25 / 10) = 3
        assertThat(response.getLogListDTO()).hasSize(1);
        verify(logMapper, times(1)).countLogList(request);
        verify(logMapper, times(1)).findLogList(request);
    }

    @Test
    @DisplayName("로그목록조회_시작일이_종료일보다_늦으면_예외가_발생한다")
    void getLogList_startDateAfterEndDate_throwsException() {
        // given
        LogRequest request = new LogRequest();
        request.setStartDate("2026-06-10 12:00:00");
        request.setEndDate("2026-06-10 11:00:00");

        // when & then
        assertThatThrownBy(() -> logService.getLogList(request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT);
        
        verify(logMapper, never()).countLogList(any(LogRequest.class));
    }

    @Test
    @DisplayName("로그목록조회_날짜_형식이_올바르지_않으면_예외가_발생한다")
    void getLogList_invalidDateFormat_throwsException() {
        // given
        LogRequest request = new LogRequest();
        request.setStartDate("2026/06/10 12:00:00"); // 잘못된 구분자
        request.setEndDate("2026-06-10 23:59:59");

        // when & then
        assertThatThrownBy(() -> logService.getLogList(request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT);

        verify(logMapper, never()).countLogList(any(LogRequest.class));
    }

    @Test
    @DisplayName("로그목록조회_페이지_사이즈가_0_이하이면_기본값인_20으로_설정된다")
    void getLogList_pageSizeZeroOrNegative_adjustsToDefaultSize() {
        // given
        LogRequest request = new LogRequest();
        request.setPage(1);
        request.setSize(0); // 0 이하 경계 조건
        request.setStartDate("2026-06-10 00:00:00");
        request.setEndDate("2026-06-10 23:59:59");

        List<LogListDTO> logs = new ArrayList<>();

        // 페이징 순서 대응: countLogList 호출 시점엔 size가 여전히 0
        given(logMapper.countLogList(request)).willReturn(15L);
        given(logMapper.findLogList(request)).willReturn(logs);

        // when
        LogResponse response = logService.getLogList(request);

        // then
        assertThat(response.getPageSize()).isEqualTo(20);
        assertThat(response.getTotalPage()).isEqualTo(1L); // ceil(15 / 20) = 1
    }

    @Test
    @DisplayName("로그단건조회_로그ID로_로그상세내역을_조회한다")
    void getLogDetail_existingLogId_returnsLogDetail() {
        // given
        String logId = "log_12345";
        LogDetailDTO detail = new LogDetailDTO();
        org.springframework.test.util.ReflectionTestUtils.setField(detail, "logId", logId);
        org.springframework.test.util.ReflectionTestUtils.setField(detail, "traceId", "trace_abc");

        given(logMapper.findByLogId(logId)).willReturn(detail);

        // when
        LogDetailDTO result = logService.getLogDetail(logId);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getLogId()).isEqualTo(logId);
        assertThat(result.getTraceId()).isEqualTo("trace_abc");
    }

    @Test
    @DisplayName("로그단건조회_해당하는_로그가_존재하지_않으면_예외가_발생한다")
    void getLogDetail_nonExistingLogId_throwsException() {
        // given
        String logId = "non_existent";
        given(logMapper.findByLogId(logId)).willReturn(null);

        // when & then
        assertThatThrownBy(() -> logService.getLogDetail(logId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOT_EXIST_LOG);
    }

    @Test
    @DisplayName("로그통계요약_통계_수치를_정상_연산하여_반환한다")
    void getLogSummary_validRequest_returnsAggregatedSummary() {
        // given
        LogSummaryRequest request = new LogSummaryRequest();
        request.setStartDate("2026-06-10 00:00:00");
        request.setEndDate("2026-06-10 23:59:59");

        // NPE 방지: summaryMap의 값들을 null이 아닌 유효 수치 객체로 Mocking
        Map<String, Object> summaryMap = new HashMap<>();
        summaryMap.put("totalCount", 100L);
        summaryMap.put("errorCount", 5L);
        summaryMap.put("avgElapsedMs", 154.67);

        given(logMapper.findLogSummary(request)).willReturn(summaryMap);

        // when
        LogSummaryResponse response = logService.getLogSummary(request);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getTotalCount()).isEqualTo(100L);
        assertThat(response.getErrorCount()).isEqualTo(5L);
        assertThat(response.getAverageElapsedMs()).isEqualTo(154.7); // 154.67 반올림 => 154.7
        assertThat(response.getSuccessRate()).isEqualTo(95.0); // ((100 - 5) / 100) * 100 => 95.0
    }

    @Test
    @DisplayName("로그통계요약_해당하는_로그가_존재하지_않으면_예외가_발생한다")
    void getLogSummary_noData_throwsException() {
        // given
        LogSummaryRequest request = new LogSummaryRequest();
        request.setStartDate("2026-06-10 00:00:00");
        request.setEndDate("2026-06-10 23:59:59");

        given(logMapper.findLogSummary(request)).willReturn(null);

        // when & then
        assertThatThrownBy(() -> logService.getLogSummary(request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOT_EXIST_LOG);
    }
}

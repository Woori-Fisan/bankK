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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LogService {

    private final LogMapper logMapper;

    private final DateTimeFormatter DATE_FORMATTER_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public LogResponse getLogList(LogRequest request) {
        log.info("[Service 시작] 거래 로그 목록 조회 로직 수행 - 요청 페이지: {}, 사이즈: {}", request.getPage(), request.getSize());

        // 입력 날짜 검증
        validateInquiryPeriod(request.getStartDate(), request.getEndDate());

        long totalCount = logMapper.countLogList(request);
        long totalPage = (long) Math.ceil((double) totalCount / request.getSize());
        log.info("[Service 로직] 전체 레코드 개수: {}, 총 페이지 수: {}", totalCount, totalPage);

        List<LogListDTO> logs = logMapper.findLogList(request);
        log.info("[Service 완료] 목록 조회 결과 건수: {}", logs.size());

        return LogResponse.builder()
                .pageNum(request.getPage())
                .pageSize(request.getSize())
                .totalPage(totalPage)
                .logListDTO(logs)
                .build();
    }

    public LogDetailDTO getLogDetail(Long id) {
        log.info("[Service 시작] 거래 로그 단건 조회 로직 수행 - ID: {}", id);

        LogDetailDTO logDetail = logMapper.findById(id);

        if (logDetail == null) {
            log.warn("[Service 경고] 해당 ID의 로그를 찾을 수 없음 - ID: {}", id);
            throw new BusinessException("해당 로그 정보를 찾을 수 없습니다.", ErrorCode.NOT_EXIST_LOG);
        }

        log.info("[Service 완료] 로그 단건 조회 결과 - ID: {}, Trace ID: {}", logDetail.getId(), logDetail.getTraceId());
        return logDetail;
    }

    public LogSummaryResponse getLogSummary(LogSummaryRequest request) {
        log.info("[Service 시작] 거래 로그 통계 요약 로직 수행");

        validateInquiryPeriod(request.getStartDate(), request.getEndDate());

        Map<String, Object> summaryMap = logMapper.findLogSummary(request);

        // 결과가 없거나 전체 건수가 0인 경우 예외 처리
        if (summaryMap == null) {
            log.warn("[Service 경고] 조회 조건에 해당하는 로그가 존재하지 않음 - 기간: {} ~ {}", request.getStartDate(), request.getEndDate());
            throw new BusinessException("해당 조건의 로그가 존재하지 않습니다.", ErrorCode.NOT_EXIST_LOG);
        }

        long totalCount = ((Number) summaryMap.getOrDefault("totalCount", 0L)).longValue();
        long errorCount = ((Number) summaryMap.getOrDefault("errorCount", 0L)).longValue();
        double avgElapsedMs = ((Number) summaryMap.getOrDefault("avgElapsedMs", 0.0)).doubleValue();

        double successRate = 0.0;
        if (totalCount > 0) {
            successRate = ((double) (totalCount - errorCount) / totalCount) * 100;
        }

        // 소수점 둘째 자리까지 반올림
        avgElapsedMs = Math.round(avgElapsedMs * 10.0) / 10.0;
        successRate = Math.round(successRate * 10.0) / 10.0;

        log.info("[Service 완료] 통계 요약 계산 완료 - 총 건수: {}, 성공률: {}%", totalCount, successRate);

        return LogSummaryResponse.builder()
                .totalCount(totalCount)
                .errorCount(errorCount)
                .averageElapsedMs(avgElapsedMs)
                .successRate(successRate)
                .build();
    }

    private void validateInquiryPeriod(String start, String end) {

        LocalDateTime startDate;
        LocalDateTime endDate;
        try {
            startDate = LocalDateTime.parse(start, DATE_FORMATTER_TIME);
            endDate = LocalDateTime.parse(end, DATE_FORMATTER_TIME);
        } catch (Exception e) {
            throw new BusinessException("날짜 형식이 올바르지 않습니다. (yyyy-MM-dd HH:mm:ss)", ErrorCode.INVALID_INPUT);
        }

        if (startDate.isAfter(endDate)) {
            throw new BusinessException("조회 시작일이 종료일보다 늦을 수 없습니다.", ErrorCode.INVALID_INPUT);
        }
    }
}

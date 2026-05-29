package com.woorifisan.monitoring.domain.log.service;

import com.woorifisan.monitoring.domain.log.dto.LogListDTO;
import com.woorifisan.monitoring.domain.log.dto.LogRequest;
import com.woorifisan.monitoring.domain.log.dto.LogResponse;
import com.woorifisan.monitoring.domain.log.mapper.LogMapper;
import com.woorifisan.monitoring.global.exception.BusinessException;
import com.woorifisan.monitoring.global.response.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

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

    public LogListDTO getLogDetail(Long id) {
        log.info("[Service 시작] 거래 로그 단건 조회 로직 수행 - ID: {}", id);

        LogListDTO logDetail = logMapper.findById(id);

        if (logDetail == null) {
            log.warn("[Service 경고] 해당 ID의 로그를 찾을 수 없음 - ID: {}", id);
            throw new BusinessException("해당 로그 정보를 찾을 수 없습니다.", ErrorCode.INVALID_INPUT);
        }

        log.info("[Service 완료] 로그 단건 조회 결과 - ID: {}, Trace ID: {}", logDetail.getId(), logDetail.getTraceId());
        return logDetail;
    }

    private void validateInquiryPeriod(String start, String end) {
        if (start == null || end == null || start.isBlank() || end.isBlank()) {
            throw new BusinessException("날짜 형식이 올바르지 않습니다. (yyyy-MM-dd HH:mm:ss)", ErrorCode.INVALID_INPUT);
        }

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

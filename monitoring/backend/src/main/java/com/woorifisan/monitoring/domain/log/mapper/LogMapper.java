package com.woorifisan.monitoring.domain.log.mapper;

import com.woorifisan.monitoring.domain.log.dto.LogDetailDTO;
import com.woorifisan.monitoring.domain.log.dto.BizLogInsertDTO;
import com.woorifisan.monitoring.domain.log.dto.LogListDTO;
import com.woorifisan.monitoring.domain.log.dto.request.LogRequest;
import com.woorifisan.monitoring.domain.log.dto.request.LogSummaryRequest;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;
import java.util.Map;

@Mapper
public interface LogMapper {
    List<LogListDTO> findLogList(LogRequest request);
    long countLogList(LogRequest request);
    LogDetailDTO findByLogId(String logId);
    Map<String, Object> findLogSummary(LogSummaryRequest request);
    void insertBizLogs(List<BizLogInsertDTO> dtos);
}

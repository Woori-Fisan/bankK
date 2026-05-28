package com.woorifisan.monitoring.domain.log.mapper;

import com.woorifisan.monitoring.domain.log.dto.LogListDTO;
import com.woorifisan.monitoring.domain.log.dto.LogRequest;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;

import java.util.List;

@Mapper
public interface LogMapper {
    List<LogListDTO> findLogList(LogRequest request);
    long countLogList(LogRequest request);
}

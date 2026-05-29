package com.woorifisan.monitoring.domain.log.dto.response;

import com.woorifisan.monitoring.domain.log.dto.LogListDTO;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class LogResponse {
    private int pageNum;
    private int pageSize;
    private long totalPage;
    private List<LogListDTO> logListDTO;
}

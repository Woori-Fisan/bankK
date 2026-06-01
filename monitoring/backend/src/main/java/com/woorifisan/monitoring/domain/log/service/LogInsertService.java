package com.woorifisan.monitoring.domain.log.service;

import com.woorifisan.monitoring.domain.log.dto.BizLogInsertDTO;
import com.woorifisan.monitoring.domain.log.mapper.LogMapper;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class LogInsertService {

    private final LogMapper logMapper;
    private final BizLogBuffer bizLogBuffer;

    @Transactional
    @Scheduled(fixedDelay = 5000)
    public void flush() {
        List<BizLogInsertDTO> batch = new ArrayList<>();
        int count = bizLogBuffer.drainTo(batch);
        if (count == 0) return;

        logMapper.insertBizLogs(batch);
        log.info("[BizLog 배치 저장] {} 건", count);
    }
}
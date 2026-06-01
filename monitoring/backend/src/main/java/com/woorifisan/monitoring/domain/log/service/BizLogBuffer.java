package com.woorifisan.monitoring.domain.log.service;

import com.woorifisan.monitoring.domain.log.dto.BizLogInsertDTO;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class BizLogBuffer {

    private final LinkedBlockingQueue<BizLogInsertDTO> queue = new LinkedBlockingQueue<>(10_000);

    public void offer(BizLogInsertDTO dto) {
        boolean accepted = queue.offer(dto);
        if (!accepted) {
            log.warn("[BizLogBuffer] 큐 초과 drop. traceId={}",
                    log.getName());
        }
    }

    public int drainTo(List<BizLogInsertDTO> target) {
        //  drainTo: atomic하게 큐를 비우므로 flush와 offer 간 race condition 없음
        return queue.drainTo(target);
    }
}
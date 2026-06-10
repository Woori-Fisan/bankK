package com.woorifisan.monitoring.domain.log.service;

import com.woorifisan.monitoring.domain.log.dto.BizLogInsertDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BizLogBufferTest {

    @Test
    @DisplayName("로그버퍼_큐에_로그를_적재하고_일괄_인출한다")
    void offerAndDrainTo_buffersAndDrainsLogsCorrectly() {
        // given
        BizLogBuffer buffer = new BizLogBuffer();
        
        // BizLogInsertDTO는 @Builder와 @Getter만 있으므로 builder 패턴을 사용하여 생성합니다.
        BizLogInsertDTO log1 = BizLogInsertDTO.builder()
                .traceId("trace-1")
                .build();
        
        BizLogInsertDTO log2 = BizLogInsertDTO.builder()
                .traceId("trace-2")
                .build();

        // when
        buffer.offer(log1);
        buffer.offer(log2);

        List<BizLogInsertDTO> target = new ArrayList<>();
        int drainedCount = buffer.drainTo(target, 10);

        // then
        assertThat(drainedCount).isEqualTo(2);
        assertThat(target).hasSize(2);
        assertThat(target.get(0).getTraceId()).isEqualTo("trace-1");
        assertThat(target.get(1).getTraceId()).isEqualTo("trace-2");

        // 큐가 비어있는지 재확인
        List<BizLogInsertDTO> emptyTarget = new ArrayList<>();
        int emptyDrained = buffer.drainTo(emptyTarget, 10);
        assertThat(emptyDrained).isEqualTo(0);
        assertThat(emptyTarget).isEmpty();
    }
}

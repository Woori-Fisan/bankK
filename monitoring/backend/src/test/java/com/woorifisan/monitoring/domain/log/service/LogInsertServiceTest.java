package com.woorifisan.monitoring.domain.log.service;

import com.woorifisan.monitoring.domain.log.dto.BizLogInsertDTO;
import com.woorifisan.monitoring.domain.log.mapper.LogMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LogInsertServiceTest {

    @InjectMocks
    private LogInsertService logInsertService;

    @Mock
    private LogMapper logMapper;

    @Mock
    private BizLogBuffer bizLogBuffer;

    @Test
    @DisplayName("배치저장_버퍼에_쌓인_로그가_존재하면_일괄_저장한다")
    void flush_hasLogsInBuffer_insertsBulk() {
        // given
        List<BizLogInsertDTO> batch = new ArrayList<>();
        batch.add(BizLogInsertDTO.builder().build());
        batch.add(BizLogInsertDTO.builder().build());

        // drainTo 호출 시 batch 리스트에 데이터를 추가하고 count(2)를 리턴하는 stubbing 설정
        given(bizLogBuffer.drainTo(anyList(), eq(500))).willAnswer(invocation -> {
            List<BizLogInsertDTO> target = invocation.getArgument(0);
            target.addAll(batch);
            return batch.size();
        });

        // when
        logInsertService.flush();

        // then
        verify(logMapper, times(1)).insertBizLogs(anyList());
    }

    @Test
    @DisplayName("배치저장_배치_저장_중_예외가_발생하면_오류로그를_출력하고_수행을_마친다")
    void flush_exceptionDuringInsert_logsAndSuppressesException() {
        // given
        List<BizLogInsertDTO> batch = new ArrayList<>();
        BizLogInsertDTO dto = BizLogInsertDTO.builder()
                .traceId("trace_111")
                .build();
        batch.add(dto);

        given(bizLogBuffer.drainTo(anyList(), eq(500))).willAnswer(invocation -> {
            List<BizLogInsertDTO> target = invocation.getArgument(0);
            target.addAll(batch);
            return batch.size();
        });

        // insertBizLogs 호출 시 예외를 던지도록 설정
        doThrow(new RuntimeException("Database is down")).when(logMapper).insertBizLogs(anyList());

        // when & then (예외가 던져지지 않고 내부적으로 catch 되어야 함)
        assertThatNoException().isThrownBy(() -> logInsertService.flush());

        verify(logMapper, times(1)).insertBizLogs(anyList());
    }

    @Test
    @DisplayName("배치저장_버퍼가_비어있으면_스킵한다")
    void flush_bufferIsEmpty_skipsInsertion() {
        // given
        given(bizLogBuffer.drainTo(anyList(), eq(500))).willReturn(0);

        // when
        logInsertService.flush();

        // then
        verify(logMapper, never()).insertBizLogs(anyList());
    }
}

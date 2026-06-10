package com.woorifisan.monitoring.domain.log.service;

import com.woorifisan.monitoring.domain.log.mapper.PartitionMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PartitionMaintenanceServiceTest {

    @InjectMocks
    private PartitionMaintenanceService partitionMaintenanceService;

    @Mock
    private PartitionMapper partitionMapper;

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter NAME_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Test
    @DisplayName("파티션관리_파티션이_존재하지_않으면_추가하고_만료파티션은_삭제한다")
    void maintain_partitionsDoNotExistAndExpiredExists_createsAndDropsPartitions() {
        // given
        LocalDate today = LocalDate.now(KST);
        
        // D+0, D+1, D+2 파티션 이름 계산
        String p0 = "p" + today.plusDays(0).format(NAME_FMT);
        String p1 = "p" + today.plusDays(1).format(NAME_FMT);
        String p2 = "p" + today.plusDays(2).format(NAME_FMT);
        
        // D-10 만료 파티션 이름 계산
        String pExpired = "p" + today.minusDays(10).format(NAME_FMT);

        // 미래 파티션은 존재하지 않음 (false)
        given(partitionMapper.partitionExists(p0)).willReturn(false);
        given(partitionMapper.partitionExists(p1)).willReturn(false);
        given(partitionMapper.partitionExists(p2)).willReturn(false);

        // 만료 파티션은 존재함 (true)
        given(partitionMapper.partitionExists(pExpired)).willReturn(true);

        // when
        partitionMaintenanceService.maintain();

        // then
        // 3개 파티션 추가 확인
        verify(partitionMapper, times(1)).addDailyPartition(p0, today.plusDays(1).format(DATE_FMT));
        verify(partitionMapper, times(1)).addDailyPartition(p1, today.plusDays(2).format(DATE_FMT));
        verify(partitionMapper, times(1)).addDailyPartition(p2, today.plusDays(3).format(DATE_FMT));
        
        // 1개 만료 파티션 삭제 확인
        verify(partitionMapper, times(1)).dropPartition(pExpired);
    }

    @Test
    @DisplayName("파티션관리_이미_파티션이_존재하거나_만료파티션이_없으면_작업을_스킵한다")
    void maintain_partitionsExistAndNoExpired_skipsDatabaseOperations() {
        // given
        LocalDate today = LocalDate.now(KST);
        
        // D+0, D+1, D+2 파티션 이름
        String p0 = "p" + today.plusDays(0).format(NAME_FMT);
        String p1 = "p" + today.plusDays(1).format(NAME_FMT);
        String p2 = "p" + today.plusDays(2).format(NAME_FMT);
        
        // D-10 만료 파티션 이름
        String pExpired = "p" + today.minusDays(10).format(NAME_FMT);

        // 미래 파티션은 이미 존재함 (true)
        given(partitionMapper.partitionExists(p0)).willReturn(true);
        given(partitionMapper.partitionExists(p1)).willReturn(true);
        given(partitionMapper.partitionExists(p2)).willReturn(true);

        // 만료 파티션은 존재하지 않음 (false)
        given(partitionMapper.partitionExists(pExpired)).willReturn(false);

        // when
        partitionMaintenanceService.maintain();

        // then
        // 추가나 삭제 동작이 전혀 수행되지 않아야 함
        verify(partitionMapper, never()).addDailyPartition(anyString(), anyString());
        verify(partitionMapper, never()).dropPartition(anyString());
    }
}

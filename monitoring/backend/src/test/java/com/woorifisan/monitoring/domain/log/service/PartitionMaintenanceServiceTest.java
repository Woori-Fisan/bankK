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

import static org.assertj.core.api.Assertions.assertThatNoException;
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
        
        String p0 = "p" + today.plusDays(0).format(NAME_FMT);
        String p1 = "p" + today.plusDays(1).format(NAME_FMT);
        String p2 = "p" + today.plusDays(2).format(NAME_FMT);
        
        String pExpired = "p" + today.minusDays(10).format(NAME_FMT);

        given(partitionMapper.partitionExists(p0)).willReturn(false);
        given(partitionMapper.partitionExists(p1)).willReturn(false);
        given(partitionMapper.partitionExists(p2)).willReturn(false);
        given(partitionMapper.partitionExists(pExpired)).willReturn(true);

        // when
        partitionMaintenanceService.maintain();

        // then
        verify(partitionMapper, times(1)).addDailyPartition(p0, today.plusDays(1).format(DATE_FMT));
        verify(partitionMapper, times(1)).addDailyPartition(p1, today.plusDays(2).format(DATE_FMT));
        verify(partitionMapper, times(1)).addDailyPartition(p2, today.plusDays(3).format(DATE_FMT));
        verify(partitionMapper, times(1)).dropPartition(pExpired);
    }

    @Test
    @DisplayName("파티션관리_이미_파티션이_존재하거나_만료파티션이_없으면_작업을_스킵한다")
    void maintain_partitionsExistAndNoExpired_skipsDatabaseOperations() {
        // given
        LocalDate today = LocalDate.now(KST);
        
        String p0 = "p" + today.plusDays(0).format(NAME_FMT);
        String p1 = "p" + today.plusDays(1).format(NAME_FMT);
        String p2 = "p" + today.plusDays(2).format(NAME_FMT);
        
        String pExpired = "p" + today.minusDays(10).format(NAME_FMT);

        given(partitionMapper.partitionExists(p0)).willReturn(true);
        given(partitionMapper.partitionExists(p1)).willReturn(true);
        given(partitionMapper.partitionExists(p2)).willReturn(true);
        given(partitionMapper.partitionExists(pExpired)).willReturn(false);

        // when
        partitionMaintenanceService.maintain();

        // then
        verify(partitionMapper, never()).addDailyPartition(anyString(), anyString());
        verify(partitionMapper, never()).dropPartition(anyString());
    }

    @Test
    @DisplayName("파티션관리_파티션_생성_중_예외가_발생해도_상위로_예외를_던지지_않는다")
    void maintain_exceptionDuringPartitionCreation_suppressesException() {
        // given
        LocalDate today = LocalDate.now(KST);
        
        String p0 = "p" + today.plusDays(0).format(NAME_FMT);
        String p1 = "p" + today.plusDays(1).format(NAME_FMT);
        String p2 = "p" + today.plusDays(2).format(NAME_FMT);
        String pExpired = "p" + today.minusDays(10).format(NAME_FMT);

        // 첫 번째 파티션 존재 여부 확인 시 false를 주어 생성을 유도함
        given(partitionMapper.partitionExists(p0)).willReturn(false);
        // 생성 도중 DB 장애 등으로 예외가 발생하는 상황 시뮬레이션
        doThrow(new RuntimeException("Partition SQL Error")).when(partitionMapper).addDailyPartition(eq(p0), anyString());

        // 나머지 파티션들은 정상 스킵되거나 검증하게 설정
        given(partitionMapper.partitionExists(p1)).willReturn(true);
        given(partitionMapper.partitionExists(p2)).willReturn(true);
        given(partitionMapper.partitionExists(pExpired)).willReturn(false);

        // when & then
        // 예외가 캐치되어 외부로 전파되지 않는지 검증
        assertThatNoException().isThrownBy(() -> partitionMaintenanceService.maintain());

        verify(partitionMapper, times(1)).addDailyPartition(eq(p0), anyString());
    }

    @Test
    @DisplayName("파티션관리_파티션_삭제_중_예외가_발생해도_상위로_예외를_던지지_않는다")
    void maintain_exceptionDuringPartitionDrop_suppressesException() {
        // given
        LocalDate today = LocalDate.now(KST);
        
        String p0 = "p" + today.plusDays(0).format(NAME_FMT);
        String p1 = "p" + today.plusDays(1).format(NAME_FMT);
        String p2 = "p" + today.plusDays(2).format(NAME_FMT);
        String pExpired = "p" + today.minusDays(10).format(NAME_FMT);

        // 미래 파티션은 이미 다 존재한다고 가정하여 스킵
        given(partitionMapper.partitionExists(p0)).willReturn(true);
        given(partitionMapper.partitionExists(p1)).willReturn(true);
        given(partitionMapper.partitionExists(p2)).willReturn(true);

        // 만료 파티션은 존재하여 삭제를 유도
        given(partitionMapper.partitionExists(pExpired)).willReturn(true);
        // 삭제 도중 예외가 발생하는 상황 시뮬레이션
        doThrow(new RuntimeException("Partition Drop SQL Error")).when(partitionMapper).dropPartition(eq(pExpired));

        // when & then
        // 예외가 캐치되어 외부로 전파되지 않는지 검증
        assertThatNoException().isThrownBy(() -> partitionMaintenanceService.maintain());

        verify(partitionMapper, times(1)).dropPartition(eq(pExpired));
    }
}

package com.woorifisan.monitoring.domain.log.service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import static org.assertj.core.api.Assertions.assertThatNoException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import org.mockito.junit.jupiter.MockitoExtension;

import com.woorifisan.monitoring.domain.log.mapper.PartitionMapper;

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
        LocalDate fixedToday = LocalDate.of(2026, 6, 10);

        try (MockedStatic<LocalDate> mockedLocalDate = mockStatic(LocalDate.class, CALLS_REAL_METHODS)) {
            // LocalDate.now(KST) 호출 시 고정된 날짜를 반환하도록 Mocking (Flaky Test 예방)
            mockedLocalDate.when(() -> LocalDate.now(KST)).thenReturn(fixedToday);

            String p0 = "p20260610";
            String p1 = "p20260611";
            String p2 = "p20260612";
            String pExpired = "p20260531"; // 10일 전: 2026-05-31

            given(partitionMapper.partitionExists(p0)).willReturn(false);

            // when
            partitionMaintenanceService.maintain();

            // then
            verify(partitionMapper, times(1)).addDailyPartition(p0, "2026-06-11");
            verify(partitionMapper, times(1)).addDailyPartition(p1, "2026-06-12");
            verify(partitionMapper, times(1)).addDailyPartition(p2, "2026-06-13");
            verify(partitionMapper, times(1)).dropPartition(pExpired);
        }
    }

    @Test
    @DisplayName("파티션관리_이미_파티션이_존재하거나_만료파티션이_없으면_작업을_스킵한다")
    void maintain_partitionsExistAndNoExpired_skipsDatabaseOperations() {
        // given
        LocalDate fixedToday = LocalDate.of(2026, 6, 10);

        try (MockedStatic<LocalDate> mockedLocalDate = mockStatic(LocalDate.class, CALLS_REAL_METHODS)) {
            mockedLocalDate.when(() -> LocalDate.now(KST)).thenReturn(fixedToday);

            String p0 = "p20260610";
            String p1 = "p20260611";
            String p2 = "p20260612";
            String pExpired = "p20260531";

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
    }

    @Test
    @DisplayName("파티션관리_파티션_생성_중_예외가_발생해도_상위로_예외를_던지지_않는다")
    void maintain_exceptionDuringPartitionCreation_suppressesException() {
        // given
        LocalDate fixedToday = LocalDate.of(2026, 6, 10);

        try (MockedStatic<LocalDate> mockedLocalDate = mockStatic(LocalDate.class, CALLS_REAL_METHODS)) {
            mockedLocalDate.when(() -> LocalDate.now(KST)).thenReturn(fixedToday);

            String p0 = "p20260610";
            String p1 = "p20260611";
            String p2 = "p20260612";
            String pExpired = "p20260531";

            given(partitionMapper.partitionExists(p0)).willReturn(false);
            doThrow(new RuntimeException("Partition SQL Error")).when(partitionMapper).addDailyPartition(eq(p0), anyString());

            given(partitionMapper.partitionExists(p1)).willReturn(true);
            given(partitionMapper.partitionExists(p2)).willReturn(true);
            given(partitionMapper.partitionExists(pExpired)).willReturn(false);

            // when & then
            assertThatNoException().isThrownBy(() -> partitionMaintenanceService.maintain());

            verify(partitionMapper, times(1)).addDailyPartition(eq(p0), anyString());
        }
    }

    @Test
    @DisplayName("파티션관리_파티션_삭제_중_예외가_발생해도_상위로_예외를_던지지_않는다")
    void maintain_exceptionDuringPartitionDrop_suppressesException() {
        // given
        LocalDate fixedToday = LocalDate.of(2026, 6, 10);

        try (MockedStatic<LocalDate> mockedLocalDate = mockStatic(LocalDate.class, CALLS_REAL_METHODS)) {
            mockedLocalDate.when(() -> LocalDate.now(KST)).thenReturn(fixedToday);

            String p0 = "p20260610";
            String p1 = "p20260611";
            String p2 = "p20260612";
            String pExpired = "p20260531";

            given(partitionMapper.partitionExists(p0)).willReturn(true);
            given(partitionMapper.partitionExists(p1)).willReturn(true);
            given(partitionMapper.partitionExists(p2)).willReturn(true);

            given(partitionMapper.partitionExists(pExpired)).willReturn(true);
            doThrow(new RuntimeException("Partition Drop SQL Error")).when(partitionMapper).dropPartition(eq(pExpired));

            // when & then
            assertThatNoException().isThrownBy(() -> partitionMaintenanceService.maintain());

            verify(partitionMapper, times(1)).dropPartition(eq(pExpired));
        }
    }
}

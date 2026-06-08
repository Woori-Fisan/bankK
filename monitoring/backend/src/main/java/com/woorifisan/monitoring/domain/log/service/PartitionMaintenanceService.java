package com.woorifisan.monitoring.domain.log.service;

import com.woorifisan.monitoring.domain.log.mapper.PartitionMapper;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PartitionMaintenanceService {

    private final PartitionMapper partitionMapper;

    private static final DateTimeFormatter NAME_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * 매일 23:00 실행.
     * - D+2 파티션을 미리 생성하여 자정 전환 시 누락 방지
     * - 10일 지난 파티션 삭제
     */
    @Scheduled(cron = "0 0 23 * * *")
    public void maintain() {
        try {
            createPartition(LocalDate.now().plusDays(2));
        } catch (Exception e) {
            log.error("[파티션 생성 실패] D+2 파티션 생성 중 오류: {}", e.getMessage(), e);
        }
        dropOldPartitions();
    }

    private void createPartition(LocalDate day) {
        String partitionName = "p" + day.format(NAME_FMT);
        String lessThanDate  = day.plusDays(1).format(DATE_FMT);
        try {
            partitionMapper.addDailyPartition(partitionName, lessThanDate);
            log.info("[파티션 생성] {} 생성 완료", partitionName);
        } catch (DataAccessException e) {
            log.warn("[파티션 생성 스킵] {} 이미 존재하거나 오류: {}", partitionName, e.getMessage());
        }
    }

    private void dropOldPartitions() {
        String cutoff = "p" + LocalDate.now().minusDays(10).format(NAME_FMT);

        List<String> targets = partitionMapper.findOldPartitionNames(cutoff);
        if (targets.isEmpty()) return;

        for (String partition : targets) {
            try {
                partitionMapper.dropOldPartition(partition);
                log.info("[파티션 삭제] {} 삭제 완료", partition);
            } catch (Exception e) {
                log.error("[파티션 삭제 실패] {}: {}", partition, e.getMessage(), e);
            }
        }

        log.info("[파티션 삭제] 총 {} 개 파티션 삭제 완료", targets.size());
    }
}
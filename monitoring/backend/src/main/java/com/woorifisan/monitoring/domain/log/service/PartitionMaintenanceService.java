package com.woorifisan.monitoring.domain.log.service;

import com.woorifisan.monitoring.domain.log.mapper.PartitionMapper;
import jakarta.annotation.PostConstruct;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PartitionMaintenanceService {

    private final PartitionMapper partitionMapper;

    private static final int RETENTION_DAYS = 10;

    private static final DateTimeFormatter NAME_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @PostConstruct
    public void init() {
        maintain();
    }

    @Scheduled(cron = "0 0 1 * * *")
    public void maintain() {
        LocalDate today = LocalDate.now();

        for (int i = 0; i <= 2; i++) {
            createPartitionIfNotExists(today.plusDays(i));
        }

        dropPartitionIfExists(today.minusDays(RETENTION_DAYS));
    }

    private void createPartitionIfNotExists(LocalDate day) {
        String partitionName = "p" + day.format(NAME_FMT);

        if (partitionMapper.partitionExists(partitionName)) {
            log.info("[파티션 생성 스킵] {} 이미 존재", partitionName);
            return;
        }

        try {
            partitionMapper.addDailyPartition(partitionName, day.plusDays(1).format(DATE_FMT));
            log.info("[파티션 생성] {} 생성 완료", partitionName);
        } catch (Exception e) {
            log.error("[파티션 생성 실패] {}: {}", partitionName, e.getMessage(), e);
        }
    }

    private void dropPartitionIfExists(LocalDate day) {
        String partitionName = "p" + day.format(NAME_FMT);

        if (!partitionMapper.partitionExists(partitionName)) {
            log.info("[파티션 삭제 스킵] {} 존재하지 않음", partitionName);
            return;
        }

        try {
            partitionMapper.dropPartition(partitionName);
            log.info("[파티션 삭제] {} 삭제 완료", partitionName);
        } catch (Exception e) {
            log.error("[파티션 삭제 실패] {}: {}", partitionName, e.getMessage(), e);
        }
    }
}
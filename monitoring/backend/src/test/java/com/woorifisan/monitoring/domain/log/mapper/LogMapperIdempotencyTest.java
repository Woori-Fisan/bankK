package com.woorifisan.monitoring.domain.log.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.woorifisan.monitoring.domain.log.dto.BizLogInsertDTO;
import com.woorifisan.monitoring.domain.log.dto.LogDetailDTO;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.test.context.ActiveProfiles;

@MybatisTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@DisplayName("LogMapper INSERT IGNORE 멱등성 검증")
class LogMapperIdempotencyTest {

    @Autowired
    private LogMapper logMapper;

    private static final LocalDateTime FIXED_TIME = LocalDateTime.of(2024, 6, 1, 12, 0, 0);

    @Test
    @DisplayName("배치 내 동일한 log_id를 가진 중복 로그는 무시된다")
    void insertBizLogs_배치내_중복_무시() {
        String duplicateLogId = "trace001_1717236000000_100000000";

        List<BizLogInsertDTO> batch = List.of(
                buildDto(duplicateLogId,                             FIXED_TIME, "INFO"),
                buildDto(duplicateLogId,                             FIXED_TIME, "INFO"),  // 완전히 동일한 중복
                buildDto("trace002_1717236000000_200000000", FIXED_TIME, "ERROR")
        );

        int inserted = logMapper.insertBizLogs(batch);

        assertThat(inserted).isEqualTo(2);

        LogDetailDTO found = logMapper.findByLogId(duplicateLogId);
        assertThat(found).isNotNull();
        assertThat(found.getLevel()).isEqualTo("INFO");
    }

    @Test
    @DisplayName("Fluent Bit 재전송으로 동일한 배치가 두 번 들어와도 중복 삽입되지 않는다")
    void insertBizLogs_배치_재전송시_중복_무시() {
        List<BizLogInsertDTO> batch = List.of(
                buildDto("trace003_1717236000000_300000000", FIXED_TIME, "INFO"),
                buildDto("trace004_1717236000000_400000000", FIXED_TIME, "ERROR")
        );

        int firstInsert  = logMapper.insertBizLogs(batch);
        int secondInsert = logMapper.insertBizLogs(batch);  // at-least-once 재전송 시뮬레이션

        assertThat(firstInsert).isEqualTo(2);
        assertThat(secondInsert).isEqualTo(0);  // 재전송 시 전부 무시
    }

    private BizLogInsertDTO buildDto(String logId, LocalDateTime timestamp, String level) {
        return BizLogInsertDTO.builder()
                .logId(logId)
                .timestamp(timestamp)
                .level(level)
                .logType("CTRL_REQ")
                .traceId(logId.split("_")[0])
                .bankCode("088")
                .build();
    }
}

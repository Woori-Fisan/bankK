-- AUTO_INCREMENT 제거 후 PK 교체를 위한 선행 작업
ALTER TABLE system_logs
    MODIFY COLUMN id BIGINT NOT NULL;

ALTER TABLE system_logs
    DROP PRIMARY KEY;

ALTER TABLE system_logs
    DROP COLUMN id;

-- log_id 를 NOT NULL 로 변경 (PK 컬럼 조건)
ALTER TABLE system_logs
    MODIFY COLUMN log_id VARCHAR(128) NOT NULL
        COMMENT '분산 로그 고유 ID (traceId_타임스탬프밀리초_나노초)';

-- 기존 idx_log_id 제거 (복합 PK의 선두 컬럼으로 커버됨)
ALTER TABLE system_logs
    DROP INDEX idx_log_id;

-- 복합 PK 설정: created_at 기준 RANGE 파티션을 위해 파티션 키 포함
-- MySQL 파티셔닝 제약: 모든 UNIQUE 인덱스는 파티션 키(created_at)를 포함해야 함
-- → log_id 단독 UNIQUE 제약은 파티션 테이블에서 불가 (애플리케이션 레벨에서 보장)
ALTER TABLE system_logs
    ADD PRIMARY KEY (log_id, created_at);
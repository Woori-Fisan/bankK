-- 1. jws_signature 컬럼 삭제, log_id 컬럼 추가
ALTER TABLE system_logs
    DROP COLUMN jws_signature,
    ADD COLUMN log_id VARCHAR(128) NULL
        COMMENT '분산 로그 고유 ID (traceId_타임스탬프밀리초_나노초)'
        AFTER id;

-- 3. AUTO_INCREMENT 제거 → PK 해제 → id 컬럼 삭제
--    MySQL 제약: MODIFY와 DROP COLUMN을 같은 컬럼에 단일 ALTER TABLE로 묶을 수 없음
ALTER TABLE system_logs
    MODIFY COLUMN id BIGINT NOT NULL;

ALTER TABLE system_logs
    DROP PRIMARY KEY;

ALTER TABLE system_logs
    DROP COLUMN id;

-- 4. 기존 데이터 전체 삭제
TRUNCATE TABLE system_logs;

-- 5. log_id NOT NULL 변경 + 복합 PK 설정
--    MySQL 파티셔닝 제약: 모든 UNIQUE 인덱스는 파티션 키(created_at)를 포함해야 함
--    → log_id 단독 UNIQUE 제약은 파티션 테이블에서 불가 (애플리케이션 레벨에서 보장)
ALTER TABLE system_logs
    MODIFY COLUMN log_id VARCHAR(128) NOT NULL
        COMMENT '분산 로그 고유 ID (traceId_타임스탬프밀리초_나노초)',
    ADD PRIMARY KEY (log_id, created_at);

-- 7. 초기 파티션 설정 (이후 파티션 관리는 PartitionMaintenanceService 가 담당)
ALTER TABLE system_logs
    PARTITION BY RANGE COLUMNS (created_at) (
        PARTITION p_future  VALUES LESS THAN (MAXVALUE)
    );
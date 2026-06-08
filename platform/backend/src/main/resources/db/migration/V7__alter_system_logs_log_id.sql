ALTER TABLE system_logs
    DROP COLUMN jws_signature;

ALTER TABLE system_logs
    ADD COLUMN log_id VARCHAR(128) NULL
        COMMENT '분산 로그 고유 ID (traceId_타임스탬프밀리초_나노초)'
        AFTER id;

ALTER TABLE system_logs
    ADD INDEX idx_log_id (log_id);
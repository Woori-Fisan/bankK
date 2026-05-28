CREATE TABLE system_logs (
    id            BIGINT          AUTO_INCREMENT PRIMARY KEY,
    created_at    DATETIME(3)     NOT NULL                    COMMENT '로그 발생 시각 (밀리초 단위)',
    level         VARCHAR(10)     NOT NULL                    COMMENT 'INFO / WARN / ERROR',
    log_type      VARCHAR(20)     NOT NULL                    COMMENT 'CONTROLLER_REQ, CONTROLLER_RES, CONTROLLER_ERR, BANK_REQ, BANK_RES, BANK_ERR, BANK_COMM_ERR',

    trace_id      VARCHAR(64)                                 COMMENT '단일 요청의 전체 흐름 추적 ID',
    staff_id      VARCHAR(50)                                 COMMENT '요청한 직원 ID',
    agency_code   VARCHAR(50)                                 COMMENT '대행기관 코드',

    bank_code     VARCHAR(10)     NOT NULL                    COMMENT '출금 은행 코드 (항상 존재)',
    target_code   VARCHAR(10)                                 COMMENT '이체 시 입금 은행 코드 (이체 외 null)',
    bank_key_id   VARCHAR(255)                                COMMENT 'RSA KEY ID (추후 구현, 현재 null)',

    http_method   VARCHAR(10)                                 COMMENT 'GET / POST',
    http_uri      VARCHAR(255)                                COMMENT '요청 URI',
    http_status   SMALLINT                                    COMMENT 'HTTP 응답 상태 코드',
    elapsed_ms    INT                                         COMMENT '처리 시간 (밀리초)',
    client_ip     VARCHAR(45)                                 COMMENT '요청 클라이언트 IP (IPv6 포함)',

    jws_signature TEXT                                        COMMENT 'JWS 전자서명 (부인방지 증거)',
    body_data     LONGTEXT                                    COMMENT '요청/응답 바디 전체 (암호화된 덩어리)',

    error_code    VARCHAR(50)                                 COMMENT '커스텀 에러 코드 (예외 시만 채움)',
    error_message TEXT                                        COMMENT '커스텀 에러 메시지 (예외 시만 채움)',

    INDEX idx_trace_id   (trace_id),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COMMENT='시스템 로그 테이블';

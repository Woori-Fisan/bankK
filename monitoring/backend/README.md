# 모니터링 서버

## 역할

`platform/backend`의 금융 거래 증적 자료로 활용되는 비즈니스 로그만 수신하여 DB에 저장하고, 로그 조회 API를 제공하는 서버입니다.  
Prometheus 메트릭 수집·Grafana 대시보드·Loki 로그 집계를 통해 플랫폼 서비스 전반의 관제 환경을 구성합니다.


---

## 기술 스택

| 구분 | 기술 |
|------|------|
| 프레임워크 | Spring Boot (Web, Security, Validation, Actuator) |
| DB | MySQL, MyBatis |
| 캐시 / 세션 | Redis — JWT 리프레시 토큰 |
| 인증 | JWT |
| 로그 집계 | Loki (S3 장기 저장) |
| 대시보드 | Grafana |

---

## 주요 기능

### 로그 수신 및 저장

Fluent Bit HTTP OUTPUT으로 전송된 플랫폼 로그를 수신해 DB에 저장합니다.

**비동기 배치 삽입 (`BizLogBuffer` + `LogInsertService`)**

- 수신한 로그를 즉시 DB에 쓰지 않고, 용량 10,000건의 `LinkedBlockingQueue`에 먼저 적재

- 5초마다 큐를 드레인하여 `INSERT` 배치 처리

- HTTP 수신 스레드와 DB 쓰기 스레드를 분리하여 수신 지연 방지

**중복 로그 멱등성 (`INSERT IGNORE`)**

- `system_logs` 테이블의 `log_id`에 UNIQUE 제약, `INSERT IGNORE`로 동일 `log_id` 재전송 시 자동 무시

- Fluent Bit 재시도로 인한 중복 수신이 DB 중복 저장으로 이어지지 않도록 보장

**MySQL 테이블 파티셔닝 (`PartitionMaintenanceService`)**

- `system_logs` 테이블을 날짜 기준으로 파티셔닝하여 대용량 로그 조회 성능 확보

- 매일 새벽 1시 파티션 자동 추가·만료 파티션 삭제

### 로그 조회 API
수집된 로그의 목록 조회·단건 상세 조회·기간별 통계 요약 API를 제공합니다.  

### 인증
- JWT 기반 인증 — DB에 등록된 유효한 사용자만 접근 허용
- 리프레시 토큰 Redis 저장, 로그아웃 시 즉시 삭제

### 관제 인프라
- Prometheus — 플랫폼·모니터링 WAS 메트릭 수집 (기본값 `:9090`)
- Grafana — Prometheus·Loki 데이터 기반 실시간 대시보드 (기본값 `:3000`)
- Loki — 로그 집계 및 인덱싱, S3 장기 아카이빙 (기본값 `:3100`)

---

## 로컬 실행

### 1. 환경변수 설정

```bash
cp .env.example .env
```

| 구분 | 변수 |
|------|------|
| **DB** | `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASS` |
| **Redis** | `REDIS_HOST`, `REDIS_PORT` |
| **JWT** | `JWT_SECRET`, `REFRESH_TOKEN_EXPIRATION_SECONDS` |
| **Loki** | `LOKI_PORT` (기본값: `3100`) |
| **Grafana** | `GRAFANA_PORT` (기본값: `3000`), `GRAFANA_ADMIN_USER`, `GRAFANA_ADMIN_PASSWORD` |
| **Prometheus** | `PROMETHEUS_PORT` (기본값: `9090`) |
| **플랫폼 연동** | `PLATFORM_BACKEND_HOST`, `PLATFORM_BACKEND_PORT` |
| **서버** | `MONITORING_BACKEND_PORT`, `CORS_ALLOWED_ORIGINS` |

> MySQL과 Redis는 별도 실행이 필요합니다. `platform/backend`의 docker-compose를 공유하거나 직접 구동하세요.

### 2. 인프라 실행

```bash
docker-compose up -d
# Loki       :3100
# Grafana    :3000
# Prometheus :9090
```

### 3. 서버 실행

```bash
./gradlew bootRun
```

Swagger UI: `http://localhost:{MONITORING_BACKEND_PORT}/swagger-ui/index.html`  
Grafana:    `http://localhost:{GRAFANA_PORT}`  
Prometheus: `http://localhost:{PROMETHEUS_PORT}`
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
| 캐시 / 세션 | Redis — JWT Refresh Token, 블랙리스트 |
| 인증 | JWT (Access 10분 / Refresh 1시간) |
| 로그 집계 | Loki (WAL 기반, 프로덕션은 S3 장기 저장 권장) |
| 대시보드 | Grafana (Prometheus·Loki 통합, iframe 임베드 지원) |
| 메트릭 | Prometheus (15초 scrape) |

---

## 주요 기능

### 로그 수신 및 저장

Fluent Bit HTTP OUTPUT으로 전송된 플랫폼 로그를 수신해 DB에 저장합니다.

**수신 엔드포인트**: `POST /api/v1/logs/biz` — 인증 불필요 (내부망 전용)

**비동기 배치 삽입 (`BizLogBuffer` + `LogInsertService`)**
- 수신한 로그를 즉시 DB에 쓰지 않고 `LinkedBlockingQueue`(용량 10,000건)에 적재
- 큐 초과 시 해당 로그 드롭 + 경고 로그 기록 (Non-blocking `offer`)
- 5초마다 큐를 드레인하여 최대 500건씩 배치 `INSERT` — HTTP 수신 스레드와 DB 쓰기 스레드 분리

**중복 로그 멱등성**
- `system_logs.log_id`에 UNIQUE 제약 + `INSERT IGNORE` — Fluent Bit 재시도로 인한 중복 수신이 DB 중복 저장으로 이어지지 않도록 보장

**MySQL 테이블 파티셔닝 (`PartitionMaintenanceService`)**
- `created_at` 기준 일자별 파티션, 파티션명 패턴 `p{yyyyMMdd}`
- 애플리케이션 기동 시 즉시 실행 + 매일 오전 1시(KST) 스케줄 실행
- 오늘 포함 3일치 파티션 사전 생성, 10일 이전 파티션 자동 삭제

---

### 로그 조회 API

| HTTP | 경로 | 설명 |
|------|------|------|
| GET | `/api/v1/monitor/transactions` | 다중 조건 목록 조회 (페이지네이션) |
| GET | `/api/v1/monitor/transactions/{logId}` | 단건 상세 조회 |
| GET | `/api/v1/monitor/summary` | 통계 집계 |

**필터 조건**: 로그 레벨 / 로그 타입 / HTTP 상태코드 / 오류 코드 / 대행기관 코드 / 직원 ID / 은행 코드 / traceId / 조회 기간(시작·종료 일시)

**통계 집계**: 총 로그 건수 / 오류 건수 / 성공률(%) / 평균 응답시간(ms) — 조회 기간·필터 조건 동일하게 적용

**페이지네이션**: 페이지당 최대 20건

---

### 인증

- **로그인**: BCrypt 비밀번호 검증 → Access Token + Refresh Token 발급, Refresh Token은 HttpOnly · Secure · SameSite=Strict 쿠키로 전달
- **Redis 저장**: `RT-MONITORING:{userId}` → Refresh Token (TTL 1시간)
- **RTR (Refresh Token Rotation)**: 토큰 갱신 시 Redis 저장값과 요청값 비교 — 불일치 시 탈취로 간주하여 세션 강제 종료 + 공격자 토큰 블랙리스트 등록
- **로그아웃**: Redis에서 Refresh Token 즉시 삭제 + Access Token을 `BLACKLIST:{token}` 키로 Redis 등록 (TTL = 남은 유효기간)
- **매 요청**: JWT 서명 검증 + 블랙리스트 여부 확인 (OncePerRequestFilter)

---

### 관제 인프라

| 컴포넌트 | 기본 포트 | 역할 |
|---------|---------|------|
| Prometheus | 9090 | 플랫폼 백엔드 메트릭 15초 주기 scrape |
| Grafana | 3000 | Prometheus·Loki 통합 대시보드, iframe 임베드 지원 |
| Loki | 3100 | 로그 집계·인덱싱, WAL로 비정상 종료 시 손실 방지 |

- Prometheus는 `platform/backend`의 `/actuator/prometheus` 엔드포인트를 scrape
- Grafana는 Prometheus·Loki를 데이터 소스로 프로비저닝, `monitoring/frontend`가 iframe으로 패널을 임베드
- Loki는 5분 미활동 또는 30분 경과 시 청크 flush, WAL 활성화로 컨테이너 재시작 후에도 로그 복구

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

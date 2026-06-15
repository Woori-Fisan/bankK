# 플랫폼 서버

## 역할

대행기관(우체국·상호금융)과 은행 코어뱅킹 사이를 중계하는 BaaS 플랫폼 서버입니다.  
창구 직원 인증·세션 관리, 은행 BaaS API 중계, 직원 보조 AI(RAG), 모니터링 연동을 담당합니다.

사용자의 민감정보(계좌번호·주민번호 등)는 프론트엔드에서 E2EE(디지털 봉투)로 암호화되어 전송됩니다.

플랫폼은 암호화된 봉투를 복호화하지 않고 원본 암호문 그대로 은행에 전달하며, 고객 원문을 취득할 수 없는 Zero-Knowledge 구조를 유지합니다.

---

## 기술 스택

| 구분 | 기술 |
|------|------|
| 프레임워크 | Spring Boot (Web, Security, Batch, Validation, Actuator) |
| DB | MySQL, MyBatis |
| 캐시 / 세션 | Redis — 리프레시 토큰, 은행 RSA 공개키, 멱등성 키 |
| 인증 | JWT (Access 10분 / Refresh 8시간) |
| 외부 API | Spring WebClient (mTLS) |
| AI / RAG | Spring AI + Gemini API + Qdrant (Vector DB) |
| 메트릭 | Spring Actuator + Micrometer → Prometheus |
| 로그 | Logback (JSON) → Fluent Bit → 모니터링 서버 |
| 보안 | mTLS (클라이언트 역할), JWS 서명 |

---

## 주요 기능

### 인증 / 세션 관리

- 사용자 Role: `AGENCY_USER` (일반 직원) / `AGENCY_ADMIN` (직원 등록·삭제 권한)
- 로그인 시 단말기 JWS 서명 검증 + Timestamp 검증(±5분)으로 Replay Attack 방지
- JWT 발급 후 Redis에 `refresh:{staffId}` → Refresh Token 저장 (TTL 8시간)
- 로그아웃 시 Redis에서 즉시 삭제 — DB 조회 없이 세션 무효화
- **RTR (Refresh Token Rotation)**: 토큰 갱신 시 Redis 저장값과 요청값이 다르면 탈취로 간주하여 전체 세션 강제 로그아웃

### BaaS API 중계

`BankExternalClient` / `BankLoanClient` (WebClient, mTLS)로 `bank/backend`에 요청을 전달합니다.

| 구분 | 플랫폼 경로 | 은행 엔드포인트 |
|------|------------|---------------|
| 잔액 조회 | `POST /bank/inquiry/balance` | `/baas/account/balance` |
| 거래 내역 | `POST /bank/inquiry/history` | `/baas/account/transactions` |
| 수취인 조회 | `POST /bank/transfer/recipient` | `/baas/transfer/recipient` |
| 이체 실행 | `POST /bank/transfer` | `/baas/transfer/execute` |
| 이체 상태 | `GET /bank/transfer/status/{bankCode}/{txId}` | `/baas/transfer/status/{txId}` |
| 출금 | `POST /bank/withdrawals` | `/baas/transfer/withdrawal` |
| 대출 심사 약관 | `GET /loan/review/documents` | `/loan/evaluation/terms` |
| 대출 심사 신청 | `POST /loan/evaluation` | `/loan/evaluation` |
| 대출 계약 약관 | `GET /loan/contract/documents` | `/loan/contract/terms/{productId}/{loanNo}` |
| 대출 실행 | `POST /loan/contract/execution` | `/loan/execution` |
| RSA 공개키 | `GET /keys/public` | `/baas/keys/public` |

- **mTLS**: PKCS12 KeyStore + TrustStore 기반 클라이언트 인증서 검증, Connect 5초 / Read 25초 타임아웃
- **JWS 서명 검증**: 요청마다 `x-jws-signature` 헤더 검증, Timestamp ±5분 초과 시 거부
- **멱등성 보장**: `X-Idempotency-Key` 기반 3단계 상태 머신
  1. Redis에 `PROCESSING` 선점 (atomic setIfAbsent, TTL 60초)
  2. 요청 처리 및 응답 캡처
  3. 성공 시 응답 본문을 Redis에 저장 / 실패 시 키 삭제 (재시도 허용)
  - 처리 중 중복 요청 → 409 / 이미 완료된 요청 → 200 + 캐시된 응답 반환

### 은행 RSA 공개키 캐싱

- 애플리케이션 기동 직후(ApplicationReadyEvent) + **매시간 정각** 스케줄 실행
- 은행 API에서 최신 Key ID + 공개키 조회 → Key ID가 변경된 경우에만 DB 업데이트
- DB 저장 후 Redis에 캐싱 — 프론트엔드 요청 시 Redis에서 즉시 반환 (E2EE 암호화 용도)

### 직원 보조 AI (RAG)

```
지식베이스(knowledge-base.md) → TokenTextSplitter (800 토큰 / 청크)
→ Gemini 임베딩 (gemini-embedding-001, 3,072차원) → Qdrant 저장

직원 질의 → 질문 임베딩 → Qdrant 유사도 검색 (상위 3개 청크)
→ 컨텍스트 조합 → Gemini API → 답변 반환
```

- 컨텍스트 외 정보는 "문서에서 찾을 수 없습니다" 응답 (Hallucination 방지)
- 애플리케이션 기동 시 기존 인덱스 삭제 후 재구성

### 모니터링 연동

- `/actuator/prometheus` → Prometheus scrape (JVM / CPU / HTTP 요청 / DB 커넥션 풀 메트릭)
- Logback JSON → AsyncAppender (큐 512) → 파일 롤링 (700MB / 파일, 14일 보관, 총 10GB 상한)
- MDC 필드: `traceId`, `staffId`, `agencyCode`, `bankCode`, `apiType`, `targetCode`, `elapsedMs`
- Fluent Bit가 `logType` 필드 기준으로 필터링 후 모니터링 서버에 HTTP 전송

---

## 로컬 실행

### 1. 환경변수 설정

```bash
cp .env.example .env
```

| 구분 | 변수 |
|------|------|
| **DB** | `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASS` |
| **Redis** | `REDIS_HOST`, `REDIS_PORT` (기본값: `6379`) |
| **JWT** | `JWT_SECRET`, `REFRESH_TOKEN_EXPIRATION_SECONDS` |
| **Qdrant** | `QDRANT_HOST`, `QDRANT_PORT` (기본값: `6333`) |
| **은행 서버** | `BANK_SERVER_URL`, `WOORI_BASE_URL`, `SHINHAN_BASE_URL`, `KB_BASE_URL` |
| **보안 키** | `PLATFORM_SECURITY_PUBLIC_KEY`, `PLATFORM_SECURITY_PRIVATE_KEY`, `TERMINAL_SECURITY_PUBLIC_KEY` |
| **Gemini** | `GOOGLE_AI_API_KEY`, `GOOGLE_CLOUD_PROJECT_ID` |
| **Fluent Bit** | `FLUENT_BIT_PORT`, `LOGGING_PATH`, `MONITORING_SERVER_IP`, `MONITORING_SERVER_PORT`, `LOKI_PORT` |
| **서버** | `PLATFORM_BACKEND_PORT` |
| **mTLS** | `SSL_KEYSTORE_PATH`, `SSL_KEYSTORE_PASSWORD`, `SSL_TRUSTSTORE_PATH`, `SSL_TRUSTSTORE_PASSWORD` |

### 2. 인프라 실행

```bash
docker-compose up -d
# MySQL    :3306
# Redis    :6379
# Qdrant   :6333
# Fluent Bit 포함
```

### 3. 서버 실행

```bash
./gradlew bootRun
```

Swagger UI: `http://localhost:{PLATFORM_BACKEND_PORT}/swagger-ui/index.html`  
Prometheus: `http://localhost:{PLATFORM_BACKEND_PORT}/actuator/prometheus`

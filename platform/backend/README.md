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
| 인증 | JWT (Access 15분 / Refresh 8시간) |
| 외부 API | Spring WebClient (mTLS) |
| AI / RAG | Spring AI + Gemini API + Qdrant (Vector DB) |
| 메트릭 | Spring Actuator + Micrometer → Prometheus |
| 로그 | Logback (JSON) → Fluent Bit → 모니터링 서버 |
| 보안 | mTLS (클라이언트 역할), JWS 서명 |

---

## 주요 기능

### 인증 / 세션 관리
- JWT 발급·갱신·로그아웃, Redis에 `staffId → refreshToken` 저장
- 로그아웃 시 Redis 즉시 삭제 — DB 조회 없이 세션 유효성 판단
- 사용자 Role: `AGENCY_USER` / `AGENCY_ADMIN` — Admin은 직원 등록·삭제 가능

### BaaS API 중계
- `BankExternalClient`(WebClient)로 `bank/backend`에 요청 전달
- **mTLS** — 클라이언트 인증서로 은행 서버에 접근, 인가된 서버만 통신 가능
- **JWS 서명** — 요청 Payload를 플랫폼 개인키로 서명하여 위변조 방지
- **멱등성 보장** — `X-Idempotency-Key` 헤더 생성·전달, Redis로 중복 거래 방지

### 은행 RSA 공개키 캐싱
- Spring Batch가 매일 1회 제휴 은행의 공개키 API를 호출하여 DB 저장 + Redis 캐싱.  
- 프론트엔드가 공개키를 요청하면 Redis에서 즉시 반환합니다. (E2EE 암호화에 사용)

### 직원 보조 AI (RAG)
```
금융 약관 PDF → 청킹(TokenTextSplitter) → 임베딩(Gemini) → Qdrant 저장
직원 질의 → 질문 임베딩 → Qdrant 유사도 검색 → Gemini API → 답변 반환
```

### 모니터링 연동
- `/actuator/prometheus` 엔드포인트로 메트릭 노출 → Prometheus scrape
- Logback JSON 로그 → Fluent Bit OUTPUT(HTTP) → 모니터링 서버 전송

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
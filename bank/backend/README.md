# 은행 서버

## 역할

은행의 원장 데이터베이스를 관리하고, 계좌 조회·이체·출금·대출 업무를 **BaaS API** 형태로 제공하는 온프레미스 코어뱅킹 서버입니다.  
`platform/backend`와 mTLS로 통신하며, 인가된 플랫폼 서버의 요청만 처리합니다.

---

## 기술 스택

| 구분 | 기술 |
|------|------|
| 언어 / 프레임워크 | Java, Spring Boot |
| DB | MySQL 8.4 — 기본 격리수준(REPEATABLE READ) + 비관적 락 |
| 캐시 | Redis — 멱등성 키 저장 전용 (TTL 1분) |
| 배치 | Spring Batch 6.0.3 — RSA 키 재발급 스케줄링 |
| 보안 | Spring Security, mTLS, JWS 서명 검증 |
| 문서 | Springdoc OpenAPI (Swagger UI) |

---

## 주요 기능

### BaaS API
| API | 설명 |
|-----|------|
| 계좌 조회 | 단일 계좌 정보·잔액 확인 |
| 거래 내역 조회 | 단일 계좌의 거래 내역 조회 |
| 수취인 확인 | 입금 대상 계좌 유효성·예금주 식별 정보 확인 |
| 이체 | 출금 원장 차감 → 입금 원장 증액 → 트랜잭션 기록 |
| 출금 | 출금 원장 차감 → 트랜잭션 기록 |
| 대출 심사 | E2EE 복호화 → 신청자 정보 확인 → CSS 신용 심사(비동기) → 가승인 한도·금리 반환 |
| 대출 진행 | 서류 해시값 무결성 확인 → 대출 최종 승인 → 원장 생성·입금 |
| 필요 서류 조회 | 대출 서류 목록 반환 |
| RSA 공개키 조회 | 플랫폼의 E2EE 암호화에 사용할 은행 RSA 공개키 반환 |

### 보안

- **mTLS** — 사전 등록된 플랫폼 클라이언트 인증서만 허용, 미인증 요청 차단

- **JWS 서명 검증** — 플랫폼 공개키로 요청 위변조 여부 검증, 실패 시 즉시 거절 + 보안 로그 기록

- **E2EE 복호화** — 은행 RSA 개인키로 1회용 AES 세션 키 복호화 → AES-GCM으로 페이로드 복호화 (Zero-Knowledge: 플랫폼은 원문 취득 불가)

- **비관적 락** — 동일 계좌 동시 출금·이체 시 갱신 손실·마이너스 잔액 방지 (`AccountConcurrencyTest`로 검증)

- **멱등성 보장** — `X-Idempotency-Key` 헤더 기반, Redis TTL 1분으로 중복 거래 방지

- **DB 암호화 (`CryptoUtil`)** — AES-256-GCM (IV 랜덤 생성, `IV + CipherText + AuthTag` 단일 컬럼 저장), ECB 모드 사용 금지

- **블라인드 인덱스** — AES-GCM의 랜덤 IV 특성상 동일 계좌번호도 매번 다른 암호문 생성 → `SHA-256(고정Salt + 계좌번호)` 해시를 별도 컬럼(`account_no_hash`)에 저장하여 WHERE 절 검색 가능하게 처리

- **계좌 비밀번호** — bcrypt 단방향 해시 저장 (SHA-256 대체 금지)

### RSA 키 로테이션

Spring Batch `RsaKeyRotationJob`이 매일 새벽 2시에 실행됩니다.  

교체 직후 1시간 동안 구 키를 병행 유지하여 플랫폼의 공개키 갱신 시간을 확보합니다.

---

## 로컬 실행

### 1. 환경변수 설정

```bash
cp .env.example .env
```

`.env`에 아래 항목을 채워주세요.

| 변수 | 설명 |
|------|------|
| `DB_HOST` / `DB_PORT` / `DB_NAME` | MySQL 접속 정보 (docker-compose 기본값: `3307`) |
| `DB_USER` / `DB_PASS` | MySQL 계정 |
| `REDIS_HOST` / `REDIS_PORT` | Redis 접속 정보 (docker-compose 기본값: `6380`) |
| `BANK_BACKEND_PORT` | 서버 포트 |
| `BANK_NAME` / `BANK_CODE` | 은행 식별 정보 |
| `DB_ENCRYPTION_KEY` | AES-256 DB 암호화 키 |
| `BLIND_INDEX_SALT` | 블라인드 인덱스용 고정 Salt |
| `DOCUMENT_STORAGE_PATH` | 대출 서류 저장 경로 |
| `SSL_SERVER_KEYSTORE_PATH` / `SSL_SERVER_KEYSTORE_PASSWORD` | mTLS 서버 키스토어 |
| `SSL_TRUSTSTORE_PATH` / `SSL_TRUSTSTORE_PASSWORD` | mTLS 트러스트스토어 |

### 2. 인프라 실행

```bash
docker-compose up -d
# MySQL :3307, Redis :6380
```

### 3. 서버 실행

```bash
./gradlew bootRun
```

Swagger UI: `http://localhost:{BANK_BACKEND_PORT}/swagger-ui/index.html`
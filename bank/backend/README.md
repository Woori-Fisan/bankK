# 은행 서버

## 역할

은행의 원장 데이터베이스를 관리하고, 계좌 조회·이체·출금·대출 업무를 **BaaS API** 형태로 제공하는 온프레미스 코어뱅킹 서버입니다.  
`platform/backend`와 mTLS로 통신하며, 인가된 플랫폼 서버의 요청만 처리합니다.

---

## 기술 스택

| 구분 | 기술 |
|------|------|
| 언어 / 프레임워크 | Java, Spring Boot |
| DB | MySQL 8.4 — REPEATABLE READ + 비관적 락 |
| 캐시 | Redis — 멱등성 키 저장 전용 (TTL 1분) |
| 보안 | Spring Security, mTLS, JWS 서명 검증 |
| 문서 | Springdoc OpenAPI (Swagger UI) |

---

## BaaS API

| HTTP | 경로 | 역할 |
|------|------|------|
| POST | `/baas/account/balance` | 잔액 조회 (E2EE) |
| POST | `/baas/account/transactions` | 거래 내역 조회 (페이징) |
| POST | `/baas/transfer/recipient` | 수취인 확인 (E2EE) |
| POST | `/baas/transfer/execute` | 이체 실행 — 당행(동기) / 타행(비동기) |
| POST | `/baas/transfer/internal/deposit` | 타행 입금 처리 (은행 간 내부 전용) |
| GET  | `/baas/transfer/status/{txId}` | 타행 이체 상태 조회 (폴링용) |
| POST | `/baas/transfer/withdrawal` | 출금 (E2EE) |
| GET  | `/loan/evaluation/terms` | 대출 심사 약관 조회 |
| POST | `/loan/evaluation` | 대출 심사 접수 (multipart: JSON + 파일) |
| GET  | `/loan/contract/terms/{productId}/{loanNo}` | 계약 약관 조회 |
| POST | `/loan/execution` | 대출 실행 (E2EE) |
| GET  | `/baas/keys/public` | 은행 RSA 공개키 조회 |
| POST | `/baas/keys` | RSA 키 등록 (관리자용) |

---

## 주요 기능

### 보안

**mTLS**
- `server.ssl.client-auth: need` — 클라이언트 인증서 없는 요청 즉시 차단
- x509 CN 추출로 플랫폼 서버 / 타행 서버 역할(Role) 구분

**JWS 서명 검증**
- 플랫폼 공개키로 요청 위변조 여부 검증
- Timestamp ±5분 초과 시 Replay Attack으로 간주하여 거부

**E2EE 복호화 및 응답 암호화**
- 요청: 은행 RSA 개인키(RSA-OAEP-256)로 1회용 AES 세션 키(CEK) 복호화 → CEK로 JWE 페이로드 복호화
- 응답: 동일 CEK를 재사용하여 민감 응답 필드를 AES-256-GCM으로 암호화 — Zero-Knowledge 원칙 유지
- 플랫폼은 원문 취득 불가, 은행만 복호화·암호화 수행

**비관적 락**
- 출금·이체·대출 실행 시 `SELECT ... FOR UPDATE`로 계좌 락 획득
- 락 획득 후 잔액 재검증 (Double-Check) — 동시 요청에 의한 마이너스 잔액 방지

**멱등성 보장**
- `X-Idempotency-Key` 헤더 기반, Redis에 처리 결과 캐싱 (TTL 1분)
- 처리 중 중복 요청 → 409 / 완료된 요청 → 200 + 캐시된 응답 반환

**DB 암호화**

| 대상 | 방식 | 적용 컬럼 |
|------|------|----------|
| 계좌번호 | AES-256-GCM (IV 랜덤, `IV + CipherText + AuthTag` 단일 저장) | `account.account_no_enc` |
| 주민번호 앞자리 | AES-256-GCM | `customer.rrn_prefix_enc` |
| RSA 개인키 | AES-256-GCM | `bank_rsa_key_ledger.private_key_enc` |
| 계좌 비밀번호 | bcrypt (단방향) | `account.password` |
| 계좌번호 검색용 | SHA-256(고정 Salt + 계좌번호) 블라인드 인덱스 | `account.account_no_hash` |

AES-GCM의 랜덤 IV 특성상 동일 계좌번호도 매번 다른 암호문이 생성되므로, WHERE 절 검색을 위해 SHA-256 해시를 별도 컬럼에 저장합니다.

---

### 이체

**당행 이체 (동기)**
```
출금 계좌 락 획득 → 출금(PENDING) → 입금 계좌 락 획득 → 입금 → 상태 SUCCESS 업데이트 → 즉시 응답
```

**타행 이체 (비동기 보상 트랜잭션)**
```
출금 계좌 락 획득 → 출금(PENDING) → 즉시 PENDING 응답 반환
  ↓ (별도 스레드)
타행 서버에 입금 요청 (mTLS) → 성공 시 SUCCESS
  실패 시 → 상태 폴링 재시도 (최대 9회, 1초 간격)
    → 최종 실패 시 출금 환불 + FAILED / 재시도 소진 시 UNKNOWN (수동 복구)
```

- 클라이언트는 `GET /baas/transfer/status/{txId}`로 결과를 폴링
- 타행 보상 스레드풀: 코어 5 / 최대 10 / 큐 50, 큐 초과 시 호출 스레드 직접 실행 (거래 유실 방지)

---

### 대출 심사 (비동기)

```
[접수] 파일 저장 → loan_ledger 생성(SUBMITTED) → 즉시 loanNo 반환
[심사] 트랜잭션 커밋 후 별도 스레드(@Async)에서 실행
  → 신용점수 조회 → DSR 계산 → 승인 한도·금리 산출
  → loan_ledger 업데이트(APPROVED / REJECTED)
  → Webhook으로 Platform에 결과 전송 (최대 3회 재시도)
[전달] Platform → SSE → 프론트엔드
```

- 심사 스레드풀: 코어 5 / 최대 20 / 큐 100
- DSR 한도 40% 기반 승인 한도 역산, 최대 1억 원
- 신용점수 600 미만 / 승인 한도 100만 원 미만 / 법정최고금리(20%) 초과 시 자동 거절

---

### RSA 키 로테이션

- `POST /baas/keys` 엔드포인트로 수동 등록 (관리자 호출)
- 키 유효기간 10년, 구 키는 만료 후에도 과거 암호화 데이터 복호화를 위해 보관
- Key ID: UUID 기반, 요청 헤더 `x-bank-key-id`로 복호화에 사용할 키 지정
- 플랫폼은 `/baas/keys/public` 조회 시 항상 최신 활성 키(valid_from 최신)를 수신

---

## 로컬 실행

### 1. 환경변수 설정

```bash
cp .env.example .env
```

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

# 은행 대리업 중계 플랫폼 백엔드

은행 대리업을 중계하는 플랫폼의 백엔드 개발에서 지켜야할 사항을 명세합니다.

## 1. 기술 스택

| 구분 | 기술 | 비고 |
|------|------|------|
| 세션 / 캐시 | Redis | |
| 인증 토큰 | JWT | |
| AI / RAG | Spring AI + Gemini API | |
| Vector DB | Qdrant | |
| 외부 API 호출 | Spring WebClient | |
| 배치 | Spring Batch 6.0.3 | |
| 메트릭 | Spring Actuator + Micrometer (Prometheus) | /actuator/prometheus 엔드포인트 노출
| 로그 수집 | Logback | Fluent Bit JSON 파싱 대상 |

## 2. mTLS

BaaS를 통해 제공되는 API와의 통신은 FAPI 표준을 준수하기 위해 mTLS를 활용합니다.
mTLS는 상호 인증서 방식의 통신으로, 서로의 인증서 정보를 확인하여 중간자 공격에 대처하기 위한 방법입니다.
미리 발급받은 인증서를 통해 은행 측과 mTLS 연결을 수행하도록 해야합니다.

## 3. JWS

API 통신을 수행함에 있어 보안 수준을 위해서 JWS를 이용한 요청을 수행합니다.
전송해야 할 정보를 Payload로 구성하고 이를 서명하여 생성한 JWS를 POST 요청 Body에 포함해 전송합니다.

## 4. 인증 / 세션 (JWT + Redis)

* Access Token 만료 시간: **15분**, Refresh Token: **8시간** (우체국 창구 업무 시간 기준)
* 토큰 발급 후 Redis에 `staffId → refreshToken` 형태로 저장합니다.
* 로그아웃 시 Redis에서 해당 키를 즉시 삭제합니다. DB 조회 없이 Redis 조회만으로 세션 유효성을 판단합니다.

## 5. RAG 처리 흐름

```
[금융 약관 PDF]
    ↓ 
문서 청킹 (TokenTextSplitter)
    ↓
임베딩 생성 (Gemini Embedding API)
    ↓
Qdrant 저장 (VectorStore)
 
[직원 질의]
    ↓
질문 임베딩 → Qdrant 유사도 검색 (Top-K 청크 조회)
    ↓
컨텍스트 + 질문 → Gemini API 전송
    ↓
답변 + 참조 청크 반환
```

## 6. 로그 수집 (Fluent Bit 연동)

Fluent Bit이 이 서버의 로그 파일을 읽어 AWS Kinesis로 전송합니다.
**로그가 JSON 형식이어야** Fluent Bit이 파싱할 수 있습니다.

## 7. JWKS 기반 은행 공개키 캐싱 (Key Vending Machine)
플랫폼 백엔드는 매번 은행에 공개키를 요청하여 발생하는 병목을 막기 위해, Spring Batch를 활용하여 하루 단위로 제휴 은행들의 공개키 반환 API를 호출합니다.
수집된 최신 은행 RSA 공개키들은 플랫폼 DB(`bank` 테이블)에 저장되고 Redis에 캐싱되며, 프론트엔드의 공개키 요청 시 캐싱된 데이터를 즉시 반환합니다.

## 8. 사용자 관리
플랫폼 사용자의 경우 AGENCY_USER, AGENCY_ADMIN으로 ROLE을 분리하며, AGENCY_ADMIN의 경우 AGENCY_USER의 등록 및 삭제할 수 있다.
로그인 진행 시 사용자의 ROLE을 판단하여 AGENCY_USER 관리 페이지 접근 권한을 확인한다.

## 9. 멱등성 보장
- **멱등성 보장 (Idempotency)** : 네트워크 지연이나 타임아웃으로 인해 동일한 이체/출금 요청을 시도하더라도, 중복 결제가 발생하지 않도록 고유 트랜잭션 ID를 생성하여 멱등성을 보장해야 한다.
- 헤더에 멱등성 키를 넣어서 요청을 보낸다.

## 10. 프로젝트 구조
```
src/main/java/com/woorifisan/platform/
├── global/
│   ├── exception/        # GlobalExceptionHandler, ErrorCode
│   ├── config/           # SecurityConfig, RedisConfig, WebClientConfig
│   └── util/             # 마스킹, 공통 유틸
│
├── auth/                 # JWT 발급, 로그인/로그아웃
│   ├── model/
│   ├── controller/
│   ├── dto/
│   ├── service/
│   └── mapper/
│
├── user/                 # AGENCY_USER, AGENCY_ADMIN
│   ├── model/
│   ├── controller/
│   ├── dto/
│   ├── service/
│   └── mapper/
│
├── bank/                 # 공개키 캐싱, JWKS 배치
│   ├── model/
│   ├── controller/
│   ├── dto/
│   ├── service/
│   └── mapper/
│
├── baas/                 # mTLS WebClient, JWS 서명
│   ├── config/
│   └── filter/
│
├── rag/                  # PDF 청킹, Qdrant, Gemini
│   ├── controller/
│   ├── dto/
│   └── service/
│
├── idempotency/          # 멱등성 키 검증
│   └── filter/
│
└── batch/                # JWKS 수집 Job
    └── job/

src/main/resources/
├── mapper/
│   ├── auth/
│   ├── user/
│   └── bank/
├── application.yml
└── logback-spring.xml
```
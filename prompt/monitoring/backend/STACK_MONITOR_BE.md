# 모니터링 백엔드(Collector) 서버

이 문서는 `monitoring/backend` 도메인 내에서 로그 수집기(Collector), 파이프라인, 그리고 시스템 메트릭 집계 서버를 개발/설정할 때 적용되는 전용 규칙입니다. (기본적인 코딩 컨벤션은 전역 `RULE_BE_STYLE.md`를 따릅니다.)

## 1. 전용 기술 스택 (Observability Stack)

| 구분 | 기술 / 서비스 | 핵심 용도 |
| :--- | :--- | :--- |
| **Log Collector** | Fluent Bit | 플랫폼 및 은행 서버에서 발생하는 애플리케이션 로그 수집 및 전처리 (단일 표준) |
| **Log Stream** | Amazon Kinesis Data Firehose | 대용량 로그 버퍼링 및 목적지(OpenSearch, S3) 실시간 라우팅 |
| **Search Engine** | Amazon OpenSearch | 실시간 에러 로그 검색 및 분석 (Hot Data) |
| **Cold Storage** | Amazon S3 | 금융 사고 대비 및 감사(Audit)를 위한 원본 로그 장기 보관 |
| **Metrics** | Prometheus & Micrometer | JVM, DB 커넥션, API 지연 시간 등 시스템 상태 지표 번역 및 수집 |
| **Tracing** | Spring Boot MDC & Micrometer Tracing | 로그에 Trace ID를 주입하여 분산 환경 추적 |

---

## 2. 모니터링 아키텍처 및 파이프라인 규칙

### 2.1. 로그 처리 파이프라인 (Log Pipeline)
* **비동기 전송**: 애플리케이션의 응답 속도에 영향을 주지 않도록, 모든 로그 수집은 비동기(Asynchronous)로 처리해야 합니다.
* **Firehose 분기 처리**: Kinesis Firehose를 통해 수집된 로그는 반드시 2개의 목적지로 분기되어야 합니다.
    1. 실시간 트러블슈팅을 위한 **OpenSearch** 적재 (최근 30일 보관)
    2. 금융감독원 감사 대비 및 데이터 백업을 위한 **S3** 적재 (LifeCycle을 통한 장기 보관)

### 2.2. 분산 추적 (Distributed Tracing)
* **MDC 기반 Trace ID 주입**: Spring Boot의 `MDC(Mapped Diagnostic Context)`를 활용해 모든 애플리케이션 로그(JSON 형식)에 `traceId` 필드를 자동으로 포함시킵니다.
* **HTTP 헤더 전파**: 플랫폼 서버에서 은행 코어 서버로 API를 호출할 때, `traceId` 흐름이 끊기지 않도록 HTTP 헤더(예: `X-B3-TraceId`)에 담아 전달합니다.
* **단일 식별자 검색**: 개발자는 OpenSearch 대시보드에서 특정 `traceId` 하나만 검색하면, 플랫폼 서버의 최초 요청 수신부터 은행 코어의 트랜잭션 처리까지 전체 흐름을 시간순으로 파악할 수 있어야 합니다.

---

## 3. 모니터링 도메인 용어 사전 (Ubiquitous Language)

AI 및 개발자는 모니터링 파이프라인과 관련된 코드/설정 작성 시 아래의 도메인 변수명을 엄격히 따릅니다.

* **분산 추적**: `traceId` (전체 요청 흐름 ID)
* **트래픽 지표**: `tps` (초당 트랜잭션 수), `activeConnections` (활성 연결 수)
* **성능 지표**: `latency` (응답 지연 시간), `cpuUsage`, `memoryUsage`
* **로그 속성**: `logLevel` (INFO, ERROR 등), `maskedPayload` (마스킹 처리된 본문), `auditLog` (감사 증적용 로그)
* **인프라 설정**: `retentionPeriod` (데이터 보존 기간), `flushInterval` (버퍼 전송 주기)

---
## 4. 인증 및 감사 (Authentication & Audit)

### 4.1. 인증 통제 (Authentication)
* 모든 API는 JWT 기반 인증을 거치며, DB에 등록된 유효한 사용자만 접근을 허용합니다.

## 5. 모니터링 로그 가공
수집된 로그를 가공된 대시보드에서 보기 쉬운 형태로 가공하여 출력한다.
### [기본 조회 목록 (Grid View)]
| 발생 일시 (KST) | 대행기관 (코드) | 담당 직원 | 거래 유형 | API 요청 경로 | 상태 | 응답 시간 | 위험도 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| `26-05-07 16:25:30` | 우체국 (`POST_001`) | EMP_998877 | **계좌이체** | `/api/v1/accounts/transfer` | **정상 (200)** | 345ms | `-` |
| `26-05-07 16:30:12` | 우체국 (`POST_001`) | EMP_998877 | **잔액조회** | `/api/v1/accounts/inquiry` | **실패 (401)** | 120ms | **주의** |
| `26-05-07 16:35:01` | 우체국 (`POST_001`) | EMP_998877 | **계좌이체** | `/api/v1/accounts/transfer` | **차단 (403)** | 45ms | **위험** |

## 6. 프로젝트 구조
```
src/main/java/com/woorifisan/monitoring/
├── global/                 # 공통 모듈 (생략)
│
└── user/                   # [User Domain]
    ├── controller/         # API 엔드포인트 (Web Layer)
    │
    ├── dto/                # 요청/응답 객체 (Application Layer용)
    │   ├── request/
    │   └── response/
    │
    ├── service/            # 비즈니스 로직 오케스트레이션
    │   └── UserApplicationService.java
    │
    ├── model/             # 핵심 도메인 모델 (기술 의존성 0)
    │
    └── mapper/         # MyBatis @Mapper 인터페이스

src/main/resources/
└── mapper/
    └── user/
        └── UserMapper.xml   # SQL 쿼리 파일
```
# [Global] AI Context & Routing Map (GEMINI.md)

본 문서는 BankBridge(BaaS 기반 은행대행업 중계 플랫폼) 모노레포에서 AI(Gemini)와 개발 팀이 작업을 수행하기 전 반드시 숙지해야 할 최상위 전역 컨텍스트이자 행동 강령입니다.

### 서비스 레이어 구조
우체국(대리업자) → 중계 플랫폼 → 은행 코어뱅킹

### 도메인별 역할

| 도메인 | 경로 | 핵심 역할 |
|--------|------|-----------|
| **platform** | `platform/` | 대행기관 직원용 웹 서비스 · 인증/세션 · 은행 API 중계 · RAG 직원 보조 |
| **bank** | `bank/` | 이체·출금·대출·조회 트랜잭션 API · 원장 관리 |
| **monitoring** | `monitoring/` | Prometheus/Grafana 기반 실시간 관측 인프라 |

---

## 1. 전역 아키텍처 및 라우팅 가이드

### 1.1. 프로젝트 모듈 개요
본 모노레포는 대행기관(우체국)과 은행 코어뱅킹을 연결하는 시스템으로 다음 3개의 모듈로 구성됩니다.

* **`platform/` (중계 플랫폼)**: 대행기관 직원용 Web, 인증/세션, 은행 API 중계, RAG 직원 보조(Gemini) 수행. **고객의 평문 개인정보(민감정보)를 데이터베이스에 절대 저장하지 않습니다.**
* **`bank/` (은행 코어 시스템)**: 이체/출금/대출 트랜잭션, 실제 원장 관리. 고객의 민감 정보는 암호화되어 이곳에만 저장됩니다.
* **`monitoring/` (관측 인프라)**: Kinesis Firehose를 통해 로그를 수집하여 OpenSearch(실시간 분석)와 S3(장기 보관 증적용)로 분기하며, CloudWatch와 Prometheus를 통해 메트릭을 수집합니다.

### 1.2. 컨텍스트 조합 규칙 (Rule of 3)
AI는 사용자의 작업 요청 도메인에 따라 아래의 **3개 파일**을 최우선으로 조합하여 컨텍스트를 구성해야 합니다.

* **[플랫폼 백엔드]**: `prompt/docs/RULE_BE_STYLE.md` + `platform/DOMAIN_PLATFORM.md` + `platform/backend/STACK_PLATFORM_BE.md`
* **[플랫폼 프론트엔드]**: `prompt/docs/RULE_FE_STYLE.md` + `platform/DOMAIN_PLATFORM.md` + `platform/frontend/STACK_PLATFORM_FE.md`
* **[은행 코어 백엔드]**: `prompt/docs/RULE_BE_STYLE.md` + `bank/DOMAIN_BANK.md` + `bank/backend/STACK_BANK_BE.md`
* **[모니터링 대시보드 FE]**: `prompt/docs/RULE_FE_STYLE.md` + `monitor/DOMAIN_MONITOR.md` + `monitor/frontend/STACK_MONITOR_FE.md`
* **[모니터링 백엔드]**: `prompt/docs/RULE_BE_STYLE.md` + `monitor/DOMAIN_MONITOR.md` + `monitor/backend/STACK_MONITOR_BE.md`

### 1.3. 작업 디렉토리 제한
사용자는 지정된 디렉토리 외의 경로는 수정하지 않으며, AI 또한 해당 경로 내에서만 코드를 생성합니다.
* **플랫폼**: `platform/backend`, `platform/frontend`
* **은행 코어**: `bank/backend`
* **모니터링**: `monitor/backend`, `monitor/frontend`

---

## 2. 핵심 보안 및 암호화 원칙 (Core Security)

AI는 코드 작성 시 구간별 보안 정책과 책임 소재 규명(면책)을 위한 아키텍처를 최우선으로 적용해야 합니다.

### 2.1. 디지털 봉투 기반 True E2EE
고객 금융 트랜잭션 발생 시, 프론트엔드는 '일회용 AES 키'를 생성해 민감 데이터를 암호화하고, 이 AES 키를 **'타겟 은행의 RSA 공개키'**로 다시 암호화하여 전송합니다.

### 2.2. Zero-Knowledge (Pass-through)
중계 플랫폼(`platform`)은 은행의 개인키가 없어 데이터를 절대 해독할 수 없으므로, 수신한 암호문 덩어리를 **복호화 없이 원본 그대로 은행 코어에 전달**합니다.

### 2.3. 자동화된 키 관리 (JWKS & Caching)
플랫폼 백엔드는 스케줄러를 통해 은행의 공개키 API를 호출하여 Redis에 캐싱하고, 프론트엔드는 이를 Session Storage에 캐싱하여 API 부하를 최소화합니다.

### 2.4. 블랙박스 로깅 (면책 사유 확보)
분쟁 해결을 위해 우체국 단말기가 생성한 **JWS 전자서명과 암호문 덩어리 원본(`req_payload`)을 `TX_PAYLOAD_LOG`에 통째로 보관**하여 위변조가 없었음을 수학적으로 증명합니다.

### 2.5. 블라인드 인덱스 (Blind Index)
은행 코어(`bank`) DB에서 암호화된 계좌번호 조회를 위해 반드시 `SHA-256(고정 Salt + 계좌번호)` 해시값을 별도 컬럼(`account_no_hash`)으로 생성하여 활용합니다.

---

## 3. AI 행동 강령 (AI Code of Conduct)

본 프로젝트에서 AI는 아래의 규칙을 엄격히 준수하여 응답해야 합니다.

### 3.1. 한국어 우선
코드 주석, 에러 메시지, 기능 설명 등 모든 텍스트 응답은 **한국어**로 작성합니다.

### 3.2. 간결하고 명확한 응답
불필요한 서론이나 사과 문구는 생략하고, 즉시 사용 가능한 코드 블록과 핵심 로직 설명 위주로 답변합니다.

### 3.3. 규칙 기반 설계
코드를 제안하기 전, 반드시 프로젝트 내의 관련 규칙 파일(`RULE_BE_STYLE.md`, `RULE_FE_STYLE.md` 등)을 먼저 참조하여 일관성을 유지합니다.
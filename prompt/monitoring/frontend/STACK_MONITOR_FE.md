# 모니터링 대시보드 프론트엔드

이 문서는 `monitoring/frontend` 도메인 내에서 관리자용 관측 대시보드 웹 애플리케이션을 개발할 때 적용되는 전용 규칙입니다. (기본적인 코딩 컨벤션 및 상태 관리 규칙은 전역 `RULE_FE_STYLE.md`를 따릅니다.)

## 1. 전용 기술 스택 (Dashboard Stack)

본 대시보드는 차트를 직접 렌더링하지 않고, 외부 **Grafana 패널을 Iframe 형태로 임베딩(Embedding)**하여 구성합니다.

| 구분 | 기술 / 라이브러리 | 핵심 용도 |
| :--- | :--- | :--- |
| **차트/시각화** | Grafana Iframe Embed | Recharts 등 별도 라이브러리 없이, 사전 구성된 Grafana 패널을 직접 호출하여 렌더링 |
| **데이터 테이블** | TanStack Table | (그라파나에서 처리하기 힘든) 상세 로그 검색 결과나 특수 데이터 표출 시에만 제한적으로 사용 |
| **URL 통신** | URL Query Parameters | 프론트엔드 상태(선택된 시간, Trace ID 등)를 Grafana Iframe URL의 변수로 전달 |

---

## 2. 모니터링 UI 및 레이아웃 정책

### 2.1. 대시보드 전용 레이아웃
* 플랫폼 대리업체 웹(`platform/frontend`)과는 별도의 독립적인 관리자 레이아웃을 가집니다.
* 좌측 LNB(Local Navigation Bar)를 통해 **메트릭 모니터링(Metrics)**, **로그 추적(Logs)**, **알람 설정(Alerts)** 메뉴로 빠르게 이동할 수 있는 SPA 구조여야 합니다.

### 2.2. 시각적 알람(Alert) 색상 표준
시스템의 상태를 직관적으로 파악할 수 있도록 데이터 시각화 시 아래의 색상 톤을 강제합니다.
* **정상 (Normal)** : Green 계열 (트래픽 원활, HTTP 200)
* **경고 (Warning)** : Yellow/Orange 계열 (지연 시간 증가, CPU/Memory 임계치 80% 근접)
* **위험 (Critical)** : Red 계열 (서버 다운, 임계치 90% 초과, HTTP 500 에러 폭증)

---

## 3. Grafana 연동 및 임베딩 규칙 (Integration)

프론트엔드 화면에 Grafana 패널을 이식하기 위해 아래의 연동 규칙을 엄격히 준수합니다.

### 3.1. Iframe 동적 URL 할당
* 프론트엔드에서 사용자가 '조회 시간(Time Range)'이나 '특정 서버(Instance)'를 변경하면, 해당 상태 값을 Grafana Iframe URL의 쿼리 파라미터로 동적 주입하여 화면을 갱신합니다.
* **URL 포맷 예시**: `src="https://grafana.domain.com/d-solo/xxx/dashboard?orgId=1&from={startTime}&to={endTime}&var-traceId={traceId}&panelId=2"`

### 3.2. 보안 및 인증 연동 (Security)
* **보안 헤더**: Grafana 서버(인프라) 측의 `grafana.ini` 설정에서 `allow_embedding = true`가 선언되어 있어야 프론트엔드에서 렌더링이 가능함을 인지하고 개발합니다.
* **인증 처리**: 프론트엔드 대시보드에 로그인한 관리자가 Grafana 화면을 볼 때 별도의 로그인 창이 뜨지 않도록, **Anonymous Auth(읽기 전용)**를 켜거나 **JWT 기반의 Auth Proxy** 연동 방식을 백엔드/인프라 팀과 협의하여 적용합니다.

### 3.3. 렌더링 최적화
* 한 페이지에 너무 많은 Iframe 패널을 띄우면 브라우저 메모리 누수 및 네트워크 커넥션(동시 호출) 제한에 걸릴 수 있습니다.
* 한 화면에 표시되는 Iframe의 개수를 최소화하고, 필요시 '전체 대시보드 임베딩'과 '개별 패널 임베딩'을 적절히 혼용하여 성능을 최적화합니다.

---

## 4. 데이터 연동 규칙 (백엔드 통합)

### 4.1. Trace ID 기반 검색 및 필터링
* 로그 검색 화면에서는 모니터링 백엔드(`STACK_MONITOR_BE.md` 참조)에서 적재한 `traceId`를 입력하여, 특정 금융 트랜잭션이 플랫폼에서 은행 코어까지 흘러간 **전체 로그 흐름을 시간순으로 정렬**하여 보여주어야 합니다.

---

## 5. 도메인 용어 사전 (대시보드 UI 기준)

컴포넌트 및 변수명 작성 시 아래의 모니터링 도메인 표준을 따릅니다.

* **시간 범위**: `timeRange`, `startDate`, `endDate` (예: 최근 30분, 1시간 단위)
* **차트 데이터**: `chartData`, `series`, `xAxis`, `yAxis`
* **지표**: `tps` (초당 트랜잭션), `latency` (응답 지연), `errorRate` (에러율)
* **로그 검색**: `searchQuery`, `logLevel` (INFO/WARN/ERROR), `traceId`

---

## 6. 접근 제어 (Access Control)

별도의 등급(Role) 체계 없이, '로그인한 사용자'만 모니터링 화면에 진입할 수 있도록 라우트를 보호합니다.

### 6.1. Protected Route 구현
* **라우트 가드**: React Router와 연동하여, 로컬 스토리지 또는 Zustand에 **유효한 JWT 토큰이 없는 사용자**가 `/monitoring/*` 경로에 직접 접근을 시도할 경우 즉시 로그인 페이지로 리다이렉트시키는 `ProtectedRoute` 컴포넌트를 사용합니다.

### 6.2. Auth Interceptor (토큰 갱신 및 만료 처리)
* Axios 인스턴스의 `request` 인터셉터에서 모든 API 호출 시 `Authorization: Bearer {JWT}` 헤더를 자동으로 포함시킵니다.
* `response` 인터셉터에서 401(Unauthorized) 응답 수신 시, 즉시 토큰을 무효화하고 경고 메시지("세션이 만료되었습니다. 다시 로그인해주세요.")와 함께 로그인 화면으로 이동시킵니다.
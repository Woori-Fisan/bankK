# monitoring/frontend

## 역할

BaaS 서비스 전반의 메트릭·로그를 관제하는 관리자용 대시보드 웹 애플리케이션입니다.  
차트·그래프는 Grafana 패널을 iframe으로 임베딩하여 렌더링하고, 상세 로그 검색은 모니터링 백엔드 API와 직접 통신합니다.

---

## 기술 스택

| 구분 | 기술 |
|------|------|
| 프레임워크 | React + TypeScript + Vite |
| 라우팅 | React Router DOM |
| 클라이언트 상태 | Zustand |
| 데이터 테이블 | TanStack Table |
| HTTP | Axios (인터셉터 기반 JWT 자동 처리) |
| 스타일 | Tailwind CSS |
| 시각화 | Grafana Iframe Embed |

---

## 화면 구성

### 로그인 / 회원가입

<img width="1917" height="982" alt="로그인 (1)" src="https://github.com/user-attachments/assets/0faecd37-3d78-41ca-8b17-c81d206e350d" />
<img width="1917" height="986" alt="회원가입" src="https://github.com/user-attachments/assets/2f2bda99-a9bd-401a-9dca-48edd18a2363" />

- JWT 기반 인증 — 유효 토큰 없는 사용자는 `ProtectedRoute`에서 로그인 페이지로 리다이렉트
- 401 응답 수신 시 Axios 인터셉터가 토큰 무효화 후 로그인 화면으로 자동 이동

---

### 로그 조회



https://github.com/user-attachments/assets/8e98a283-d927-4e09-8b03-bc81de6cb7ab



- TanStack Table로 로그 목록 렌더링 (페이지네이션·필터·정렬)
- `traceId` 검색 → 플랫폼→은행 전 구간 요청 흐름을 시간순으로 추적 가능
- 위험도 컬럼 색상 코딩으로 이상 거래 즉시 식별
- 로그 리스트 및 개별 로그 상세 내역 PDF 출력 기능 제공

---

### 로그 모니터링



https://github.com/user-attachments/assets/4aadbf62-0bea-4ac8-a08b-798117b1155c



- Grafana 패널을 iframe으로 임베딩하여 렌더링
- 사용자가 조회 시간 범위를 변경하면 iframe URL 쿼리 파라미터를 동적 갱신하여 패널 새로고침
  ```
  /d-solo/xxx/dashboard?from={startTime}&to={endTime}&var-instance={server}&panelId=2
  ```
- 로깅 요소: 전체로그 타임라인, Error 건수, 전체 로그 리스트, 비즈니스 로그 리스트, 비즈니스 로그 건수
- 상태별 색상 기준: 정상 **Green** / 경고 **Yellow·Orange** / 위험 **Red**

---

### 메트릭 모니터링



https://github.com/user-attachments/assets/f6072591-f83c-47c1-a1c2-b8b75fc28ef9


- 메트릭 요소: 각 api 뱔 TPS(초당 트랜잭션 수), 응답 시간, JWM 힙 메모리 사용량, DB 커넥션

---


## 로컬 실행

```bash
# 의존성 설치
npm install

# 개발 서버 실행
npm run dev
```

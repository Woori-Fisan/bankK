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

![로그인](./docs/screenshots/login.png)

> 📸 _스크린샷 자리: 로그인 폼_

- JWT 기반 인증 — 유효 토큰 없는 사용자는 `ProtectedRoute`에서 로그인 페이지로 리다이렉트
- 401 응답 수신 시 Axios 인터셉터가 토큰 무효화 후 로그인 화면으로 자동 이동

---

### 로그 조회

<img width="1917" height="982" alt="1  Log 로그 조회" src="https://github.com/user-attachments/assets/86772766-1c3b-47d2-85d1-4a639ccda289" />

<img width="1917" height="980" alt="2  로그 상세 정보 조회" src="https://github.com/user-attachments/assets/3efc94e0-f3ff-4ca5-9bf8-bade2896244f" />


- TanStack Table로 로그 목록 렌더링 (페이지네이션·필터·정렬)
- `traceId` 검색 → 플랫폼→은행 전 구간 요청 흐름을 시간순으로 추적 가능
- 위험도 컬럼 색상 코딩으로 이상 거래 즉시 식별

---

### 로그 모니터링

<img width="1917" height="977" alt="1  실시간 로그" src="https://github.com/user-attachments/assets/84ac3d49-925f-486a-b03f-82eed42b2459" />


- Grafana 패널을 iframe으로 임베딩하여 렌더링
- 사용자가 조회 시간 범위를 변경하면 iframe URL 쿼리 파라미터를 동적 갱신하여 패널 새로고침
  ```
  /d-solo/xxx/dashboard?from={startTime}&to={endTime}&var-instance={server}&panelId=2
  ```
- 상태별 색상 기준: 정상 **Green** / 경고 **Yellow·Orange** / 위험 **Red**

---

### 메트릭 모니터링

<img width="1917" height="982" alt="1  실시간 메트릭 조회" src="https://github.com/user-attachments/assets/91d80fce-41bf-4c82-bd68-6182f65f220f" />


- Grafana 패널을 iframe으로 임베딩하여 렌더링
- 사용자가 조회 시간 범위를 변경하면 iframe URL 쿼리 파라미터를 동적 갱신하여 패널 새로고침
  ```
  /d-solo/xxx/dashboard?from={startTime}&to={endTime}&var-instance={server}&panelId=2
  ```
- 상태별 색상 기준: 정상 **Green** / 경고 **Yellow·Orange** / 위험 **Red**

---


## 로컬 실행

```bash
# 의존성 설치
npm install

# 개발 서버 실행
npm run dev
```

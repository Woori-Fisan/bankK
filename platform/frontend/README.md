# platform/frontend

## 역할

창구 직원이 은행 업무(계좌 조회·이체·출금·대출)를 처리하는 Web UI입니다.  
민감정보는 서버에 도달하기 전 브라우저에서 직접 암호화되며, 플랫폼 서버는 원문을 취득할 수 없는 **Zero-Knowledge 구조**를 클라이언트에서 구현합니다.

---

## 기술 스택

| 구분 | 기술 |
|------|------|
| 프레임워크 | React + TypeScript + Vite |
| 라우팅 | React Router DOM v7 |
| 서버 상태 | TanStack Query (비동기 데이터 패칭·폴링) |
| 클라이언트 상태 | Zustand |
| HTTP | Axios |
| 스타일 | Tailwind CSS + shadcn/ui |
| 암호화 | Web Crypto API + `jose` (RSA/AES E2EE) |

---

## 화면 구성

### 로그인

![로그인 화면](./docs/screenshots/login.png)

> 📸 _스크린샷 자리: 로그인 폼_

- 비밀번호는 플랫폼 RSA 공개키로 암호화하여 전송 (`useRsaEncrypt`)
- 로그인 시 은행 RSA 공개키를 1회 수신 → Zustand `bankKeyStore`에 캐싱 (거래마다 재요청 금지)

---

### 메인 (업무 선택)

![메인 화면](./docs/screenshots/main.png)

> 📸 _스크린샷 자리: 업무 선택 메뉴_

---

### 계좌 조회

https://github.com/user-attachments/assets/8edb3ac9-8ba0-4961-ac56-a27180b782ca


계좌번호를 입력하면 잔액과 거래 내역을 조회합니다.

- 조회 기관: 드롭다운으로 연결된 은행 기관을 선택
- 기본 조회 기간: 최근 1달 (오늘 기준 -1개월), 날짜 직접 변경 가능
- 입출금 구분: 출금(KRW) · 입금(KRW) 컬럼 분리 — 음수(−) 금액은 출금 컬럼에 빨간색, 양수(+) 금액은 입금 컬럼에 초록색으로 표시
- 거래 건별 "입금" / "출금" 배지 자동 표시
- 페이지네이션 지원 (20건/페이지)
---

### 이체

https://github.com/user-attachments/assets/67e48fdb-0f1e-4501-aa3d-783009f5824c


Step 흐름:
```
계좌 입력 → 수취인 확인 → 금액 / 비밀번호 입력 → 결과
```

---

### 출금



https://github.com/user-attachments/assets/643b26dc-b75c-4175-9f42-10e4d2dd9de5



Step 흐름:
```
계좌 입력 → 잔액 확인 → 금액 / 비밀번호 입력 → 결과
```

---

### 대출 신청



https://github.com/user-attachments/assets/eeb1aace-28dd-4856-be3a-fd172db2237f



Step 흐름:
```
서류 제출 → 심사 대기 (Polling) → 상품 선택 → 계약 서류 확인 → 실행 → 결과
```

- 심사는 비동기 처리 — TanStack Query `refetchInterval: 5000`으로 5초마다 상태 조회
- `PENDING` 이 아닌 응답이 오면 폴링 자동 중단

---

### 직원 관리 (AGENCY_ADMIN 전용)



https://github.com/user-attachments/assets/744db4ff-f3cb-4ba1-9faf-f5a5b0c3ed8c



---

### 챗봇



https://github.com/user-attachments/assets/bb126781-ce40-4d11-8caf-13f4b7bb8beb


---


## 주요 구현

### 디지털 봉투 암호화 (`useHybridEncrypt`)

금융 트랜잭션 요청 시 브라우저에서 직접 E2EE를 수행합니다.

```
1. 1회용 AES-256 세션 키 로컬 생성 (Web Crypto API)
2. 계좌번호·금액 등 민감 페이로드를 AES 키로 암호화
3. AES 키 + 계좌 비밀번호를 은행 RSA 공개키로 암호화
4. 암호화된 봉투를 플랫폼 서버에 전달
   → 플랫폼은 원문 취득 불가, 은행만 복호화 가능
```

- 은행 RSA 공개키 교체 시 `Key ID`를 명시하여 키 교체 시점 이전 요청도 처리 보장

---

### 핀패드 모달

| 기능 | 설명 |
|------|------|
| **랜덤 셔플** | 렌더링마다 숫자 배열 무작위 재배치 |
| **멀티 버튼 하이라이트** | 실제 입력 버튼 1개 + 무작위 2~3개 버튼이 동시에 active — 어깨너머·화면 녹화로 번호 유추 방지 |
| **입력값 격리** | 입력값은 컴포넌트 내부 state에만 존재, 완료 시 즉시 암호화 후 암호문만 콜백 전달 |

![핀패드](./docs/screenshots/pinpad.png)

> 📸 _스크린샷 자리: 핀패드 모달_

---

## 로컬 실행

```bash
# 의존성 설치
npm install

# 개발 서버 실행
npm run dev
```

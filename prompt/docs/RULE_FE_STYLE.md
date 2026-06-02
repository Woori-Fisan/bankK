# [Global] 프론트엔드 코딩 컨벤션 및 규칙 (RULE_FE_STYLE.md)

본 문서는 BankBridge 프로젝트의 모든 프론트엔드 애플리케이션(`platform/frontend`, `monitoring/frontend`) 개발 시 반드시 준수해야 하는 공통 규칙입니다.

## 1. 기술 스택

| 구분 | 기술 | 비고 |
|------|------|------|
| UI 프레임워크 | React 18 | Vite 기반, CSR |
| 언어 | JavaScript (ES2022+) | TypeScript 미사용 |
| 상태 관리 | Zustand | 클라이언트 상태 전용 |
| 서버 상태 | TanStack Query (React Query) | API 캐싱·동기화 |
| HTTP 클라이언트 | Axios | 인스턴스 단일화 |
| 스타일링 | Tailwind CSS v3 | |

## 2. 페이지 디자인 및 레이아웃

컴포넌트와 페이지를 디자인할 때, 사용자가 제시하는 이미지 파일 경로를 확인하여 같은 형태로 개발해야 합니다.

---

## 3. SPA(Single Page Application)

페이지는 React 기반의 SPA 형태로 구성되어야 합니다.
로그인 페이지를 제외한 페이지의 경우는 일정한 레이아웃이 존재하며, 페이지 컨텐츠 위치에 페이지가 포함되어야 합니다.


## 4. 네이밍 컨벤션 (Naming Conventions)

### 4.1. 파일 및 폴더

* **컴포넌트 파일**: PascalCase를 사용합니다. (예: `TransferForm.jsx`, `PinPadModal.jsx`)
* **훅 파일**: camelCase, `use` 접두사를 반드시 붙입니다. (예: `useRsaEncrypt.js`, `useMaskInput.js`)
* **유틸·API 파일**: camelCase를 사용합니다. (예: `formatter.js`, `bankApi.js`)
* **폴더명**: 모두 소문자 camelCase를 사용합니다. (예: `pages/transferPage/`, `components/pinpad/`)

### 4.2. 변수 및 함수

* **일반 변수·함수**: camelCase. (예: `accountNumber`, `handleSubmit`)
* **컴포넌트 함수**: PascalCase. (예: `function TransferForm() {}`)
* **상수**: UPPER_SNAKE_CASE. (예: `MAX_PIN_RETRY`, `SESSION_TIMEOUT_MS`)
* **이벤트 핸들러**: `handle` 접두사를 사용합니다. (예: `handleSubmit`, `handleAmountChange`)
* **boolean 변수**: `is`, `has`, `can` 접두사를 사용합니다. (예: `isLoading`, `isPinPadOpen`)
---

## 5. 폴더 구조 원칙

```
src/
├── pages/          # 라우트 단위 페이지. 1 라우트 = 1 폴더
├── components/     # 재사용 UI 컴포넌트. 비즈니스 로직 포함 금지
│   ├── common/     # 프로젝트 전역 공용 (Button, Input, Modal 등)
│   └── {domain}/   # 특정 기능 전용 컴포넌트 (pinpad/, assistant/ 등)
├── hooks/          # 커스텀 훅. 상태 및 사이드이펙트 캡슐화
├── api/            # Axios 인스턴스 및 API 호출 함수만 위치
├── store/          # 전역 상태
└── utils/          # 순수 함수 유틸리티 (포맷터, 유효성 검사 등)
```

* **페이지 컴포넌트**는 레이아웃 조합과 훅 호출만 담당합니다. 직접 `axios`를 호출하거나 비즈니스 로직을 작성하지 않습니다.
* **`api/` 폴더** 외부에서 Axios 인스턴스를 직접 생성하거나 호출하는 것을 금지합니다.
* **`components/`** 는 `api/`를 직접 import 하지 않습니다. API 호출은 반드시 훅(`hooks/`)을 통해 간접 호출합니다.
---
## 6. 컴포넌트 작성 규칙

* **함수형 컴포넌트만 사용**합니다. 클래스형 컴포넌트 작성을 금지합니다.
* **Props 구조 분해**: 컴포넌트 인자에서 즉시 구조 분해하여 사용합니다.
  ```jsx
  // ❌
  function AmountInput(props) {
    return <input value={props.value} onChange={props.onChange} />;
  }
 
  // ⭕
  function AmountInput({ value, onChange }) {
    return <input value={value} onChange={onChange} />;
  }
  ```
* **단일 책임 원칙**: 하나의 컴포넌트는 하나의 역할만 합니다. 컴포넌트가 100줄을 넘어가면 분리를 검토합니다.
* **JSX 조건부 렌더링**: 삼항 연산자는 1depth까지만 허용합니다. 복잡한 조건은 변수로 분리하거나 컴포넌트로 추출합니다.
  ```jsx
  // ❌ 중첩 삼항
  {isLoading ? <Spinner /> : hasError ? <ErrorMsg /> : <Form />}
 
  // ⭕ 변수 분리
  const content = isLoading ? <Spinner /> : <Form />;
  return <>{content}</>;
  ```

---

## 7. 상태 관리 원칙 (State Management)

* **상태 범위 최소화**: 상태는 그것을 필요로 하는 컴포넌트에 최대한 가깝게 선언합니다. 전역 상태 남용을 금지합니다.
* **전역 상태 기준**: 여러 페이지에 걸쳐 공유되거나, 페이지 이동 후에도 유지되어야 하는 상태만 전역 Store에 올립니다.
    * ⭕ 전역 Store 사용: 직원 인증 정보(JWT), 진행 중인 거래 상태
    * ❌ 전역 Store 남용: 특정 페이지의 입력 폼 값, UI 토글 상태
* **서버 상태와 클라이언트 상태를 분리**합니다. API 응답 데이터(서버 상태)는 전역 Store에 직접 저장하지 않고, 컴포넌트 내 `useState` 또는 별도 서버 상태 관리 방식으로 처리합니다.
---

## 8. API 호출 규칙

* **Axios 인스턴스 단일화**: `api/axiosInstance.js` 하나의 인스턴스만 사용합니다. 컴포넌트나 훅에서 `axios.create()`를 직접 호출하는 것을 금지합니다.
* **인터셉터 집중 관리**: `Authorization` 헤더, `X-JWS-Signature` 서명, 공통 에러 처리는 모두 `axiosInstance.js`의 인터셉터에서만 처리합니다. 개별 API 함수에서 헤더를 직접 세팅하지 않습니다.
* **API 함수 명명**: HTTP 메서드가 아닌 비즈니스 행위를 명확히 드러냅니다.
    * ❌ `postTransfer()`, `getBalance()`
    * ⭕ `executeTransfer()`, `fetchBalance()`
* **공통 에러 응답 처리**: API 호출 실패 시 공통 에러 포맷(`{ success, error: { code, message } }`)을 기준으로 처리합니다. 각 호출부에서 `error.response.data`를 직접 파싱하지 않습니다.
---

## 9. 유효성 검사 (Validation)

* **제출 전 클라이언트 검증**: 폼 제출(`handleSubmit`) 시점에 반드시 입력값 유효성을 검사합니다. 유효하지 않은 값은 API를 호출하기 전에 차단합니다.
* **유틸 함수 분리**: 계좌번호 형식, 주민번호 형식 등 금융 입력값 검증 로직은 `utils/validator.js`에 순수 함수로 분리합니다. 컴포넌트 내부에 정규식을 직접 작성하는 것을 금지합니다.
  ```
  // ⭕ utils/validator.js
  export const isValidAccountNumber = (value) => /^\d{10,14}$/.test(value);
 
  // ❌ 컴포넌트 내부에 직접 작성
  if (!/^\d{10,14}$/.test(accountNumber)) { ... }
  ```
* **실시간 피드백**: 사용자가 입력을 완료한 시점(`onBlur`)에 인라인 에러 메시지를 노출합니다. `onChange` 시점의 과도한 검증 노출은 지양합니다.
---

## 10. 금융 데이터 처리 규칙 (Finance-Specific)

이 섹션의 규칙은 금융 서비스 특성상 **보안 및 법적 요건**에 해당하므로 예외 없이 준수합니다.

### 10.1. 디지털 봉투 기반 하이브리드 암호화 (True E2EE)
* 은행 코어로 전송되는 민감 데이터(계좌번호, 주민번호, 금액 등)는 순수 RSA가 아닌 **디지털 봉투(Digital Envelope)** 방식으로 암호화해야 합니다.
* **진행 순서 (`useHybridEncrypt` 훅 등 공통 유틸 사용):**
    1. 트랜잭션 발생 시 1회용 AES-256 세션 키를 로컬에서 생성합니다.
    2. 고객의 민감 페이로드를 해당 AES 키로 암호화합니다.
    3. 세션스토리지에 캐싱해둔 **[타겟 은행의 RSA 공개키]**를 사용하여, '1회용 AES 키'와 '계좌 비밀번호'를 암호화합니다.
* 암호화되지 않은 평문을 `store`, `state`, `log`에 저장하거나 출력하는 것을 절대 금지합니다.

### 10.2. 금액 표시

* 금액은 화면에 표시할 때 반드시 세 자리 콤마(,)를 포함한 포맷을 적용합니다.
* `utils/formatter.js`의 `formatAmount()` 함수를 사용합니다. 컴포넌트에서 직접 `toLocaleString()`을 호출하는 것을 지양합니다.
  ```js
  // ⭕
  import { formatAmount } from '@/utils/formatter';
  <span>{formatAmount(balance)}</span>  // "1,234,567"
 
  // ❌
  <span>{balance.toLocaleString()}</span>
  ```

---

## 11. 세션 및 보안 관리 (Session & Security)

모든 프론트엔드 애플리케이션은 사용자의 인증 상태를 안전하게 관리하고, 비정상적인 접근을 차단하기 위해 아래의 세션 정책을 엄격히 준수합니다.

### 11.1. 세션 타임아웃 (Session Timeout)
* **비활동 자동 로그아웃**: 사용자의 마지막 활동(클릭, 키보드 입력 등)으로부터 **30분(변경 가능)**이 경과하면 세션을 자동으로 만료시키고 로그인 페이지로 리다이렉트합니다.
* **구현 방식**: `useSessionTimeout` 훅을 전역 레이아웃에 배치하여 사용자의 이벤트를 감지하고, 타이머 만료 시 Zustand의 `logout()` 액션을 호출합니다.

### 11.2. 브라우저 종료 시 토큰 무효화
* **휘발성 저장소 권장**: JWT 토큰은 브라우저 종료 시 자동으로 삭제되는 `sessionStorage`에 보관하는 것을 원칙으로 합니다.
* **종료 감지**: 보안상 중요한 페이지(대시보드 등)에서는 `beforeunload` 이벤트를 사용하여 사용자가 브라우저를 닫을 때 로컬의 인증 정보를 즉시 파기(Clear Storage)합니다.

### 11.3. 로그아웃 처리 프로세스
* **Frontend**: 스토리지 초기화(`localStorage.clear()`, `sessionStorage.clear()`) 및 Zustand 스토어 리셋을 수행합니다.
* **Backend 연동**: 로그아웃 버튼 클릭 시 반드시 백엔드의 로그아웃 API를 호출하여 서버 측 세션(또는 토큰 블랙리스트)을 즉시 무효화해야 합니다.

## 12. 웹 표준 및 시맨틱 마크업 (Web Standards & Semantic Markup)

모든 UI 컴포넌트는 웹 표준을 엄격히 준수하며, 브라우저와 보조 기기(스크린 리더 등)가 페이지 구조를 정확히 이해할 수 있도록 의미에 맞는 마크업을 작성해야 합니다.

### 12.1. 시맨틱 태그(Semantic Tag) 적극 활용
* 무분별한 `<div>`와 `<span>` 남용을 엄격히 금지합니다.
* 페이지 레이아웃과 콘텐츠의 의미에 맞춰 `<header>`, `<nav>`, `<main>`, `<section>`, `<article>`, `<aside>`, `<footer>` 등의 시맨틱 태그를 적재적소에 사용합니다.

### 12.2. 목적에 맞는 인터랙티브 요소 사용 (Button vs Link)
* **페이지 이동**: 클릭 시 다른 페이지나 외부 URL로 이동하는 경우 반드시 `<a>` 태그(또는 React Router의 `<Link>`)를 사용합니다.
* **상태 변경 및 동작**: 클릭 시 모달이 열리거나, API가 호출되거나, 데이터가 제출되는 등 화면 내 액션이 발생할 경우 반드시 `<button>` 태그를 사용합니다.
* 모든 `<button>` 요소에는 예기치 않은 폼 제출을 막기 위해 `type="button"` 또는 `type="submit"` 속성을 명확히 기재해야 합니다.

### 12.3. 웹 접근성 (Accessibility, A11y) 기본 수칙
* **대체 텍스트**: 모든 `<img>` 태그에는 이미지 로드 실패 및 스크린 리더 사용자를 위한 `alt` 속성을 반드시 작성합니다. (순수 장식용 이미지는 읽히지 않도록 `alt=""` 처리)
* **폼(Form) 레이블링**: `<input>` 요소에는 반드시 짝을 이루는 `<label>` 요소를 제공(`htmlFor`와 `id` 매핑)하여, 사용자가 입력 필드의 목적을 명확히 알 수 있도록 구성합니다.


## 13. 테스트 (Testing)

* **도구**: 단위 테스트는 Vitest + React Testing Library, E2E 테스트는 Cypress를 사용합니다.
* **단위 테스트 대상**: `utils/`, `hooks/` 디렉터리를 최우선으로 작성하며, 커버리지 80% 이상을 유지합니다. 테스트 파일은 대상 파일과 같은 경로에 `{filename}.test.js`로 위치시킵니다.
* **E2E 테스트 대상**: 로그인·이체·출금·대출 등 핵심 금융 플로우를 대상으로 하며, `cypress/e2e/`에 위치시킵니다. API 안정화 이후 작성합니다.
* **민감 데이터**: 테스트용 계좌번호·비밀번호는 `cypress.env.json`에 분리하고 Git에 포함하지 않습니다.
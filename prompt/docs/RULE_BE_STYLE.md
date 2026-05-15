# [Global] 백엔드 코딩 컨벤션 및 규칙 (RULE_BE_STYLE.md)

본 문서는 BankBridge 프로젝트의 모든 백엔드 애플리케이션(`platform/backend`, `bank/backend`, `monitoring/backend`) 개발 시 반드시 준수해야 하는 공통 규칙입니다.

## 1. 기술 스택

| 구분 | 기술 |
|------|------|
| 언어 | Java 17 |
| 프레임워크 | Spring Boot 3.5.14 |
| ORM | JDBC, MyBatis |
| 보안 | Spring Security 7.0.5 |
| 유효성 검사 | jakarta.validation |
| DB | MySQL 8.4 |
| 빌드 | Gradle |

## 2. 네이밍 컨벤션 (Naming Conventions)

### 2.1. 클래스명 작성 가이드
- Controller : `{Domain}Controller` (예: `AccountController`)
- Service : `{Domain}Service` (예: `TransferService`)
- Repository : `{Entity}Mapper` (예: `AccountMapper`)
- 공통 모듈 : `{Function}Manager` / `{Function}Verifier`
- DTO (요청/응답) : `{Domain}Request` / `{Domain}Response`
- 커스텀 예외 : `{Domain}Exception`
- Config : `{Feature}Config` (예: `SecurityConfig`)
- Filter / Interceptor : `{Function}Filter` / `{Function}Interceptor`

### 2.2. 메서드명 작성 가이드
- 조회 (단건/목록) : `get{Target}` / `find{Target}List`
- 검증 / 실행 : `verify{Target}`, `validate{Target}` / `execute{Action}`
- 저장 / 계산 : `save{Target}` / `calculate{Target}`
- 차감 / 증가 / 변환 : `deduct{Target}` / `increase{Target}` / `to{Target}`

### 2.3. 일반 변수명 가이드
- 기본형 : 카멜케이스 사용 (예: `requestAmount`)
- boolean : `is{State}` / `has{Target}` (예: `isLocked`, `hasOverdraft`)
- 컬렉션 : `{target}List` / `{target}Map` (복수형 `s` 대신 `List` 접미사 권장)
- 상수 : 대문자 스네이크케이스 (예: `MAX_LOAN_RATE`)

---

## 3. 공통 응답 규격 (API Response)

모든 API의 응답은 컨트롤러에서 임의의 객체나 `Map`을 반환하지 않으며, 반드시 사전에 정의된 공통 래퍼 클래스인 `ApiResponse<T>`를 사용하여 반환합니다.

* **정상 응답 구조**: `ApiResponse.success(T data)` 형태의 정적 팩토리 메서드를 사용합니다.
    * 내부 필드: `success` (true), `data` (실제 응답 객체), `error` (null)
* **금기 사항**: `ResponseEntity`의 body에 DTO를 직접 넣거나 String을 반환하는 것을 엄격히 금지합니다.

### 3.1. API 문서화 (Swagger / SpringDoc)

* **라이브러리**: `springdoc-openapi` 를 사용합니다.
* **접근 제한**: Swagger UI(`/swagger-ui/**`, `/v3/api-docs/**`)는 **개발·스테이징 환경에서만 노출**합니다. 운영 환경에서는 Spring Security로 차단합니다.
* **공통 응답 래퍼 연동**: `ApiResponse<T>`가 Swagger에서 올바르게 렌더링되도록 `@OpenAPIDefinition` 전역 설정에 공통 스키마를 등록합니다.
* 하나의 API에서 발생 가능한 에러가 복수인 경우, `@ApiResponse`를 직접 나열하지 않고 커스텀 어노테이션 `@ApiErrorResponses`를 사용합니다.

---

## 4. 예외 처리 (Exception Handling)

### 4.1. 글로벌 예외 처리
* 컨트롤러나 서비스 로직에서 `try-catch`로 예외를 삼키지(Swallow) 않습니다.
* 발생하는 모든 비즈니스 예외는 `@RestControllerAdvice`가 적용된 `GlobalExceptionHandler`에서 중앙 집중식으로 캐치하여 처리합니다.

### 4.2. 에러 코드 관리 (Enum)
* 매직 스트링(문자열 하드코딩) 사용을 금지합니다.
* 모든 에러 코드와 메시지는 반드시 `ErrorCode` Enum 클래스에 정의하여 중앙 관리합니다. (예: `INSUFFICIENT_BALANCE("ERR_001", "출금 가능한 잔액이 부족합니다.")`)

### 4.3. 커스텀 예외 및 에러 응답 구조
* 비즈니스 로직 오류 시 `ErrorCode`를 필드로 갖는 `BusinessException` (또는 이를 상속받은 커스텀 예외)을 `throw` 합니다.
* 예외 발생 시 프론트엔드로 반환되는 에러 응답은 반드시 `ApiResponse.error(ErrorCode)` 정적 팩토리 메서드를 통해 아래의 클래스 구조로 래핑하여 반환해야 합니다.
    * 내부 필드: `success` (false), `data` (null), `error` (`ErrorResponse` 객체)
    * `ErrorResponse` 필드: `code` (String, Enum의 코드값), `message` (String, Enum의 메시지)

---

## 5. 트랜잭션 및 데이터 접근 로직 (Transaction)

* **조회 전용 튜닝**: 데이터를 조회만 하는 서비스 메서드에는 반드시 `@Transactional(readOnly = true)`를 선언하여 DB 성능 최적화를 도모합니다.
* **외부 API 호출 분리**: 서비스 메서드 내부에 외부 플랫폼 API 호출(HTTP Request)이 포함될 경우, 해당 구간은 가급적 `@Transactional` 범위에서 제외하여 DB 커넥션 풀과 락(Lock) 점유 시간을 최소화합니다.

---

## 6. 유효성 검사 (Validation)

* **DTO 검증**: 컨트롤러에 도달하기 전, DTO 클래스 내부에 `jakarta.validation` 어노테이션(`@NotBlank`, `@Min`, `@Pattern` 등)을 사용하여 필수 값과 형식을 검증합니다.
* 컨트롤러의 파라미터에는 `@Valid`를 명시하여, 실패 시 `MethodArgumentNotValidException`이 글로벌 예외 처리기로 넘어가도록 구성합니다.

---

## 7. 폴더 및 패키지 구조 (Package Structure)

모든 백엔드 모듈은 DDD(Domain Driven Design) 패턴을 기반으로 하며, 역할에 따라 엄격히 분리합니다. AI는 새로운 파일 생성 시 아래 구조를 벗어나지 않아야 합니다.

### 7.1. 기본 패키지 경로
`com.woorifisan.{module}.{domain}`

### 7.2. 계층별 역할 정의
* **`controller/` (표현 계층)**
    * 클라이언트의 HTTP 요청을 받고 응답(API)을 반환합니다.
    * ❌ 비즈니스 로직을 포함해서는 안 됩니다.
* **`dto/` (데이터 전송 객체)**
    * API의 Request, Response 객체 및 내부 계층 간 데이터 전달용 클래스가 위치합니다.
* **`service/` (비즈니스 계층)**
    * 핵심 비즈니스 로직을 수행하며, `@Transactional`을 통해 트랜잭션 경계를 관리합니다.
    * 여러 Repository를 조합하여 도메인 요구사항을 해결합니다.

### 7.3. Entity 정적 팩토리 메서드

* Entity 생성 시 `new` 키워드를 직접 사용하지 않고, 반드시 `of()` 또는 `from()` 정적 팩토리 메서드를 통해 생성합니다.

---
## 8. 테스트 코드 (Testing)
* 단위 테스트는 `JUnit5`와 `Mockito`를 사용하며, BDD 스타일의 `Given-When-Then` 구조를 엄격히 준수합니다.
* 테스트 메서드의 이름은 검증하고자 하는 행위와 결과를 명확히 알 수 있도록 **한글**로 작성합니다. (예: `void 잔액이_부족하면_출금_예외가_발생한다()`)
* 돌연변이 테스트를 위해 `pitest`를 사용합니다.
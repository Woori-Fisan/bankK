# [우리FISA 6기] 클라우드 서비스 개발 과정 5팀

## 1\. 프로젝트 개요
  * **주제**: BaaS 기반 은행대리업 중계 서비스
  * **프로젝트 기획 배경**: 
     - 은행 지점의 지속적인 감소로 고령층·지방 거주자 등 금융 취약계층의 오프라인 은행 접근성이 저하되고 있습니다. 정부는 이에 대한 포용 금융 대응책으로 은행대리업을 제시했으며, 2025년 12월 금융규제 샌드박스를 통해 혁신금융서비스로 지정되었습니다.
     - 기존 방식은 은행과 대행기관이 1:1 전용망을 구축해야 해 과도한 개발 비용과 시간이 발생했습니다.
     - BankK는 BaaS를 중계 레이어로 활용하여 우체국·상호금융 등 대행기관이 단일 플랫폼을 통해 여신 상품 판매·계좌 조회·출금 업무를 처리할 수 있도록 하며, 신규 대행기관 추가 시 별도 개발 없이 설정만으로 연결을 확장합니다.
  * **기술 스택**
      * 백엔드: Java 17, Spring Boot 3.2, MySQL 8.0, Redis, MyBatis, Spring Security, Spring AI, Spring Batch, Spring WebClient (mTLS), Logback, Micrometer
      * 프론트엔드: React, TypeScript, Vite, TanStack Query, Zustand, Axios, Tailwind CSS, shadcn/ui, Web Crypto API, jose
   
        
## 2\. 아키텍쳐

### 2-1. 시스템 아키텍쳐
<img width="8683" height="6242" alt="아키텍처(PROD)" src="https://github.com/user-attachments/assets/be9718ce-e3df-4e60-8e8c-f265d20de85a" />

### 설명
- 대행기관의 창구 직원용 Web(platform/frontend)과 관제용 대시보드(monitoring/frontend)는 각각 독립적으로 배포되며, 외부 요청은 플랫폼 서버를 통해서만 은행 코어로 전달됩니다.
- platform/backend는 인증·보안·중계를 전담하고, bank/backend는 온프레미스 환경에서 실제 원장 데이터를 처리하며, monitoring/backend는 전 구간 거래 로그를 수집·관제합니다.
- 각 서버는 독립적으로 배포·확장 가능하며, 인프라는 컨테이너 기반으로 구성되어 신규 대행기관 추가 시 설정 변경만으로 연결을 확장합니다.

---

### 2-2. 소프트웨어 아키텍처
<img width="1712" height="1494" alt="image" src="https://github.com/user-attachments/assets/c312ab83-9371-455c-b788-b0fef2b68cbc" />

### 설명
- 대행기관(우체국·상호금융)의 창구 직원이 Web 프론트엔드를 통해 요청을 보내면, platform/backend가 중간에서 인증·보안 처리를 담당하며 은행 코어뱅킹과 중계합니다.
- platform과 bank/backend 간 통신은 mTLS + JWS 서명 + E2EE(디지털 봉투) 3중 보안 구조로 보호되며, 플랫폼은 암호문을 복호화하지 않고 그대로 전달하는 Zero-Knowledge 원칙을 유지합니다.
- bank/backend는 플랫폼으로부터 받은 암호화 페이로드를 직접 복호화하여 계좌 조회·이체·대출 등 코어뱅킹 비즈니스 로직을 처리하고, 민감 데이터는 AES-256으로 암호화해 저장합니다.
- 모든 거래 요청은 traceId 기반 단일 추적 ID로 전 구간 로깅되어 Fluent Bit를 통해 monitoring/backend로 수집되며, 관제 대시보드에서 실시간 로그·메트릭·성공률을 확인할 수 있습니다.

---

### 2-3. ERD 다이어그램
- Platform ERD
<img width="1260" height="884" alt="image" src="https://github.com/user-attachments/assets/4a8149e6-f786-4c71-89cd-9be5b0271297" />

- 대행기관(agency)·직원(staff)·은행(bank) 정보를 관리하며, 은행별 RSA 공개키를 캐싱하여 프론트엔드의 E2EE 암호화에 활용합니다.
- 직원 로그인 시 JWT Refresh Token은 Redis에 저장되며, 멱등성 키도 Redis를 통해 관리하여 중복 거래를 원천 차단합니다.


- Bank ERD
<img width="2278" height="1186" alt="image" src="https://github.com/user-attachments/assets/6e84326f-3b30-4167-9705-98532151b6ad" />

- 고객(customer)·계좌(account)·거래원장(transaction_ledger)·대출(loan_ledger) 테이블로 구성되며, 계좌번호·주민번호 등 민감 정보는 AES-256-GCM으로 암호화하여 저장합니다.
- 계좌번호 검색을 위해 SHA-256 블라인드 인덱스를 별도 컬럼에 유지하여, 암호화된 상태에서도 WHERE 절 조회가 가능하도록 설계했습니다.

---

## 3\. 주요 기능 소개

### 3-1. 핵심 기술 구성
<img width="1640" height="425" alt="image" src="https://github.com/user-attachments/assets/c0987d31-e0f0-43bd-bade-29d088dc758e" />


### 3-2. 통합 워크플로우 다이어그램
<img width="3454" height="2633" alt="api흐름_120배율 (1)" src="https://github.com/user-attachments/assets/133f49dd-19b0-409c-aaeb-5184450efca5" />


### 3-3. 세부 기능 소개

#### [기능 1] 3중 보안 구조 — mTLS + JWS 서명 + E2EE (디지털 봉투)
 
* **기능 설명**: 금융 사고 발생 시 소속 은행이 "주의 의무를 다했음"을 기술적으로 증명하기 위해 통신 구간·페이로드·종단 간 3중 보안을 적용했습니다. mTLS로 플랫폼과 은행 서버 간 양방향 인증서를 검증하여 인가된 서버만 접근할 수 있도록 하고, JWS 서명으로 요청 페이로드의 위변조 여부와 Replay Attack(±5분 Timestamp 검증)을 차단합니다. E2EE(디지털 봉투)는 브라우저에서 1회용 AES-256 세션 키로 민감 정보를 암호화한 뒤 AES 키를 은행 RSA 공개키로 래핑하여 전송하며, 플랫폼 서버는 암호문을 **복호화하지 않고 원본 그대로 은행에 전달(Zero-Knowledge)**합니다.
* **핵심 코드**:
```java
// bank/backend — JWS 서명 검증 (global/security)
String jwsSignature = request.getHeader("x-jws-signature");
JWSObject jwsObject = JWSObject.parse(jwsSignature);
JWSVerifier verifier = new RSASSAVerifier(platformPublicKey);
if (!jwsObject.verify(verifier)) throw new BusinessException(ErrorCode.INVALID_JWS_SIGNATURE);
 
// Replay Attack 방지: Timestamp ±5분 초과 시 거부
long requestTime = Long.parseLong(jwsObject.getPayload().toJSONObject().get("timestamp").toString());
if (Math.abs(System.currentTimeMillis() - requestTime) > 300_000)
    throw new BusinessException(ErrorCode.REPLAY_ATTACK_DETECTED);
```
* **코드 링크**: `bank/backend/global/security`
---
 
#### [기능 2] 브라우저 E2EE — 클라이언트 사이드 디지털 봉투 암호화
 
* **기능 설명**: 계좌번호·주민번호·비밀번호 등 민감 정보는 서버에 도달하기 전 브라우저에서 직접 암호화됩니다. Web Crypto API로 1회용 AES-256-GCM 세션 키를 생성하고, 민감 페이로드를 세션 키로 암호화한 뒤 세션 키 자체를 은행 RSA 공개키(RSA-OAEP-256)로 래핑합니다. 이 구조 덕분에 플랫폼 서버와 네트워크 구간 모두에서 원문 취득이 불가능하며, 오직 해당 은행의 RSA 개인키를 보유한 서버만 복호화할 수 있습니다. 대출 서류 파일(PDF)도 동일한 방식으로 암호화하여 전송합니다.
* **핵심 코드**:
```typescript
// platform/frontend — 디지털 봉투 암호화 (useHybridEncrypt)
const aesKey = await crypto.subtle.generateKey(
  { name: 'AES-GCM', length: 256 }, true, ['encrypt', 'decrypt']
);
const iv = crypto.getRandomValues(new Uint8Array(12));
const encryptedPayload = await crypto.subtle.encrypt(
  { name: 'AES-GCM', iv }, aesKey, sensitiveData
);
// AES 키를 은행 RSA 공개키로 래핑
const exportedKey = await crypto.subtle.exportKey('raw', aesKey);
const wrappedKey = await crypto.subtle.encrypt(
  { name: 'RSA-OAEP' }, bankPublicKey, exportedKey
);
return { reqPayload: encryptedPayload, wrappedKey, iv, bankKeyId };
```
* **코드 링크**: `platform/frontend/hooks/useHybridEncrypt`
---
 
#### [기능 3] Redis 기반 멱등성 보장 — 중복 거래 원천 차단
 
* **기능 설명**: 네트워크 타임아웃이나 사용자의 재시도로 인한 이중 출금·이중 이체를 막기 위해, 클라이언트가 요청마다 고유한 `X-Idempotency-Key`를 생성하여 헤더에 포함합니다. 서버는 Redis의 `SETNX`(원자적 SET if Not eXists)로 해당 키를 선점하고, 최초 처리 결과를 캐싱합니다. 동일 키로 재요청이 들어오면 DB 접근 없이 즉시 캐시된 응답을 반환하며, 처리 중인 요청이 중복으로 들어올 경우 409를 반환합니다. 플랫폼과 은행 서버 양쪽에 각각 적용되어 이중 방어를 구성합니다.
* **핵심 코드**:
```java
// platform/backend — 멱등성 필터 (global/idempotency/filter)
String idempotencyKey = "idempotency:" + key;
Boolean isNew = redisTemplate.opsForValue()
    .setIfAbsent(idempotencyKey, "PROCESSING", 60, TimeUnit.SECONDS);
 
if (Boolean.FALSE.equals(isNew)) {
    String cached = redisTemplate.opsForValue().get(idempotencyKey);
    if ("PROCESSING".equals(cached)) return response409();  // 처리 중
    return writeCachedResponse(cached);                      // 완료된 요청 → 캐시 반환
}
// 처리 완료 후 결과 저장
redisTemplate.opsForValue().set(idempotencyKey, responseBody, 60, TimeUnit.SECONDS);
```
* **코드 링크**: `platform/backend/global/idempotency/filter`
---
 
#### [기능 4] 비관적 락 + 보상 트랜잭션 — 동시성 제어 및 타행 이체 안전성
 
* **기능 설명**: 출금·이체·대출 실행 시 `SELECT ... FOR UPDATE`로 계좌 락을 획득하고, 락 획득 후 잔액을 재검증(Double-Check)하여 동시 요청에 의한 마이너스 잔액을 원천 차단합니다. 타행 이체는 분산 트랜잭션 문제를 보상 트랜잭션 패턴으로 해결합니다. 출금 후 즉시 `PENDING` 응답을 반환하고, 별도 스레드에서 타행 서버에 입금을 요청합니다. 실패 시 최대 9회 재시도(1초 간격)를 수행하며, 최종 실패 시 출금을 환불하고 `FAILED`로 기록합니다. 클라이언트는 폴링 API로 결과를 확인합니다.
* **핵심 코드**:
```java
// bank/backend — 비관적 락 + Double-Check (AccountMapper)
@Select("SELECT * FROM account WHERE id = #{id} FOR UPDATE")
Account findByIdForUpdate(Long id);
 
// TransferService — 보상 트랜잭션 (타행 이체 실패 시 환불)
@Async("transferExecutor")
public void executeInterBankTransferAsync(TransferContext ctx) {
    for (int attempt = 0; attempt < MAX_RETRY; attempt++) {
        try {
            interBankClient.requestDeposit(ctx);
            updateStatus(ctx.getTxId(), "SUCCESS"); return;
        } catch (Exception e) {
            Thread.sleep(1000);
        }
    }
    // 최종 실패 → 출금 환불
    accountService.refund(ctx.getWithdrawalAccountId(), ctx.getAmount());
    updateStatus(ctx.getTxId(), "FAILED");
}
```
* **코드 링크**: `bank/backend/domain/account/service/TransferService`
---
 
#### [기능 5] 부인방지 로그 파이프라인 — 전 구간 거래 추적
 
* **기능 설명**: 금융 분쟁 발생 시 귀책사유를 소명하기 위해 모든 거래 요청에 `traceId`(traceId + epoch + nano + rand4hex)를 부여하고, 대행기관→플랫폼→은행 전 구간에 단일 ID를 전파합니다. 플랫폼 서버는 AOP(`ControllerLoggingAspect`)로 컨트롤러 진입·종료 시점에 MDC에 staffId·agencyCode·bankCode·elapsedMs를 함께 기록하며, 위변조 증명을 위한 암호문 원본(`TX_PAYLOAD_LOG`)을 별도 테이블에 적재합니다. Logback JSON → AsyncAppender → Fluent Bit → monitoring/backend 파이프라인으로 수집되며, MySQL 테이블 파티셔닝(일자별)으로 대용량 로그를 관리합니다.
* **핵심 코드**:
```java
// platform/backend — ControllerLoggingAspect (global/aop/aspect)
@Around("controllerMethods() && !excludedMethods()")
public Object logAround(ProceedingJoinPoint joinPoint) throws Throwable {
    String traceId = generateTraceId(); // traceId + epoch + nano + rand4hex
    MDC.put("traceId", traceId);
    MDC.put("staffId", SecurityContextHolder.getContext()...);
    MDC.put("agencyCode", ...);
    long start = System.currentTimeMillis();
    try {
        Object result = joinPoint.proceed();
        MDC.put("elapsedMs", String.valueOf(System.currentTimeMillis() - start));
        return result;
    } finally {
        MDC.clear();
    }
}
```
* **코드 링크**: `platform/backend/global/aop/aspect/ControllerLoggingAspect`

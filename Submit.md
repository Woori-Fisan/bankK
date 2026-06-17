# [우리FISA 6기] 클라우드 서비스 개발 과정 5팀

## 1\. 프로젝트 개요
  * **주제**: BaaS 기반 은행대리업 중계 서비스
  * **프로젝트 기획 배경**: 
     - 은행 지점의 지속적인 감소로 고령층·지방 거주자 등 금융 취약계층의 오프라인 은행 접근성이 저하되고 있습니다. 정부는 이에 대한 포용 금융 대응책으로 은행대리업을 제시했으며, 2025년 12월 금융규제 샌드박스를 통해 혁신금융서비스로 지정되었습니다.
     - BankK는 BaaS를 중계 레이어로 활용하여 우체국·상호금융 등 대행기관이 단일 플랫폼을 통해 대출 상품 판매·계좌 조회·출금 업무를 처리할 수 있도록 하며, 신규 대행기관 추가 시 은행과 대행기관이 1:1 전용망을 구축하는 별도 비용 없이 설정만으로 연결을 확장합니다.
     - 은행법 제43조의20의 손해배상 리스크를 선제적으로 설계에 반영하여, 은행업무 위탁 과정에서 발생하는 모든 거래 로그를 수집·보관함으로써 금융 사고 발생 시 면책력을 입증할 수 있는 구조적 정당성을 확보합니다.
    
        > 은행법 제43조의20 (손해배상책임): 관리 감독 의무는 원천적으로 위탁은행(시중은행)이 일방 부담하게 되어 있다.
      
  * **기술 스택**
      * 백엔드: Java 17, Spring Boot 3.2, MySQL 8.0, Redis, MyBatis, Spring Security, Spring AI, Spring Batch, Spring WebClient, Logback, Micrometer
      * 프론트엔드: React, TypeScript, Vite, TanStack Query, Zustand, Axios, Tailwind CSS, shadcn/ui, Web Crypto API, jose
   
        
## 2\. 아키텍쳐

### 2-1. 시스템 아키텍처
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
- 모든 거래 요청은 traceId 기반 단일 추적 ID로 전 구간 로깅되어 Fluent Bit를 통해 monitoring/backend로 수집되며, 관제 대시보드에서 실시간 로그 및 메트릭을 확인할 수 있습니다.

---

### 2-3. ERD 다이어그램
- **Platform ERD**
<img width="1260" height="884" alt="image" src="https://github.com/user-attachments/assets/4a8149e6-f786-4c71-89cd-9be5b0271297" />

    대행기관(agency)·직원(staff)·은행(bank) 정보를 관리하며, 은행별 RSA 공개키를 프론트엔드의 E2EE 암호화에 활용합니다.
    직원 로그인 시 JWT Refresh Token은 Redis에 저장되며, 멱등성 키도 Redis를 통해 관리하여 중복 거래를 원천 차단합니다.


- **Bank ERD**
<img width="2278" height="1186" alt="image" src="https://github.com/user-attachments/assets/6e84326f-3b30-4167-9705-98532151b6ad" />

    고객(customer)·계좌(account)·거래원장(transaction_ledger)·대출(loan_ledger) 테이블로 구성되며, 계좌번호·주민번호 등 민감 정보는 AES-256-GCM으로 암호화하여 저장합니다.
    계좌번호 검색을 위해 SHA-256 블라인드 인덱스를 별도 컬럼에 유지하여, 암호화된 상태에서도 WHERE 절 조회가 가능하도록 설계했습니다.

---

## 3\. 주요 기능 소개

### 3-1. 핵심 기술 구성
<img width="1130" height="926" alt="image" src="https://github.com/user-attachments/assets/a616fd7e-1be5-4d60-8494-674d4936aa96" />

---

### 3-2. 통합 워크플로우 다이어그램
<img width="4047" height="5333" alt="image" src="https://github.com/user-attachments/assets/56715b4a-8ac8-4719-b3db-af3cab7ec384" />


---


### 3-3. 세부 기능 소개
 
#### [기능 1] 3중 보안 구조 — mTLS + JWS 서명 + E2EE (디지털 봉투)

* **기능 설명**: 금융 사고 발생 시 소속 은행이 **"주의 의무를 다했음"**을 기술적으로 증명하기 위해 **통신 구간·페이로드·종단 간 3중 보안을 적용**했습니다.

  * **mTLS**: **플랫폼과 은행 서버 간 양방향 인증서**를 검증하여 인가된 서버만 접근할 수 있도록 합니다.
  * **JWS 서명**: 단말기(창구 PC)가 IndexedDB에 저장된 RSA 개인키로 요청 페이로드에 전자서명하고, 플랫폼 서버가 이를 검증합니다. Timestamp ±5분 초과 요청은 Replay Attack으로 간주하여 거부합니다.
  * **E2EE (디지털 봉투)**: 계좌번호·주민번호·비밀번호 등 민감 정보는 브라우저에서 직접 암호화됩니다. Web Crypto API로 1회용 AES-256-GCM 세션 키를 생성해 민감 페이로드를 암호화하고, 세션 키 자체는 은행 RSA 공개키(RSA-OAEP-256)로 래핑합니다. 플랫폼 서버는 암호문을 **복호화하지 않고 원본 그대로 은행에 전달(Zero-Knowledge)**하며, 오직 해당 은행의 RSA 개인키를 보유한 서버만 복호화할 수 있습니다.

* **핵심 코드**:
```java
// platform/backend — JWS 서명 검증 (global/util/CryptoUtil)
public static String verifyJwsAndGetPayload(String jwsString, String publicKeyPem) {
    RSAPublicKey publicKey = parsePublicKey(publicKeyPem);
    JWSObject jwsObject = JWSObject.parse(jwsString);
    if (jwsObject.verify(new RSASSAVerifier(publicKey))) {
        return jwsObject.getPayload().toString();
    }
    return null;
}
// Replay Attack 방지: Timestamp ±5분 초과 시 거부
long requestTime = Long.parseLong(payload.get("timestamp").toString());
if (Math.abs(System.currentTimeMillis() - requestTime) > 300_000)
    throw new BusinessException(ErrorCode.REPLAY_ATTACK_DETECTED);
```
```typescript
// platform/frontend — 디지털 봉투 암호화 (utils/bankCrypto.ts · hybridEncrypt)
const aesKey = await window.crypto.subtle.generateKey(
  { name: 'AES-GCM', length: 256 }, true, ['encrypt', 'decrypt']
);
const iv = window.crypto.getRandomValues(new Uint8Array(12));
const encryptedData = await window.crypto.subtle.encrypt(
  { name: 'AES-GCM', iv, additionalData: new TextEncoder().encode(header) },
  aesKey, new TextEncoder().encode(JSON.stringify(sensitiveData))
);
// AES 키(CEK)를 은행 RSA 공개키(RSA-OAEP-256)로 래핑 → JWE Compact 직렬화
const encryptedKey = await window.crypto.subtle.encrypt(
  { name: 'RSA-OAEP' }, bankPublicKey,
  await window.crypto.subtle.exportKey('raw', aesKey)
);
return { reqPayload: `${header}.${encKey}.${ivStr}.${cipherStr}.${tagStr}`, aesKey };
```
* **코드 링크**: [CryptoUtil.java](https://github.com/Woori-Fisan/bankK/blob/develop/platform/backend/src/main/java/com/woorifisan/platform/global/util/CryptoUtil.java) · [bankCrypto.ts](https://github.com/Woori-Fisan/bankK/blob/develop/platform/frontend/src/utils/bankCrypto.ts)

---
 
#### [기능 2] 대출 심사 비동기 처리 — SSE 실시간 푸시 + Webhook + Polling 복구

* **기능 설명**: 대출 심사는 수십 초가 걸리는 비동기 작업이므로, 단말기와의 동기 연결을 즉시 끊고 결과를 실시간으로 밀어주는 구조를 설계했습니다. 단말기가 [신청하기]를 누르는 순간 고유한 `requestKey`로 SSE 채널을 열고, 플랫폼은 은행에 심사를 접수한 뒤 즉시 `SUBMITTED`를 반환하여 스레드를 해제합니다. 은행 내부 심사가 완료되면 플랫폼의 Webhook API를 호출하고, 플랫폼은 결과를 **Redis에 먼저 캐싱한 뒤** 열려 있는 SSE 채널로 단말기에 결과를 Push합니다. 네트워크 불안정으로 SSE 연결이 끊어진 경우에도, 단말기가 결과 조회 API를 Polling하면 Redis 캐시에서 결과를 반환하여 정합성을 보장합니다.

* **핵심 코드**:
```java
// platform/backend — SSE 구독 및 ConcurrentHashMap 관리 (domain/loan/service/LoanService)
private final ConcurrentHashMap<String, SseEmitter> pendingEmitters = new ConcurrentHashMap<>();

public SseEmitter subscribe(String requestKey) {
    SseEmitter emitter = new SseEmitter(120_000L); // 120초 타임아웃
    pendingEmitters.put(requestKey, emitter);

    emitter.onTimeout(() -> { pendingEmitters.remove(requestKey); emitter.complete(); });
    emitter.onCompletion(() -> pendingEmitters.remove(requestKey));
    emitter.onError(e -> pendingEmitters.remove(requestKey));

    emitter.send(SseEmitter.event().name("connect").data("connected")); // 초기 이벤트 (프록시 타임아웃 방지)
    return emitter;
}

// 15초마다 heartbeat 전송 — Nginx 등 프록시 유휴 연결 강제 종료 방지
@Scheduled(fixedRate = 15_000)
public void sendHeartbeats() {
    pendingEmitters.forEach((key, emitter) -> {
        try { emitter.send(SseEmitter.event().name("heartbeat").data("ping")); }
        catch (Exception e) { pendingEmitters.remove(key); emitter.complete(); }
    });
}

// Webhook 수신 — Redis 먼저 저장 후 SSE push (연결 단절 시 polling 복구 보장)
public void handleCallback(LoanCallbackRequest callback, String secret) {
    if (!webhookSecret.equals(secret))
        throw new BusinessException(ErrorCode.LOAN_WEBHOOK_SECRET_INVALID); // X-Webhook-Secret 검증

    // SSE 연결 유무와 무관하게 Redis에 결과 먼저 저장 (TTL 300초)
    redisTemplate.opsForValue().set(
        LOAN_RESULT_KEY_PREFIX + callback.getRequestKey(),
        objectMapper.writeValueAsString(result), 300L, TimeUnit.SECONDS
    );

    SseEmitter emitter = pendingEmitters.remove(callback.getRequestKey());
    if (emitter != null) {
        emitter.send(SseEmitter.event().name("result").data(objectMapper.writeValueAsString(result)));
        emitter.complete();
    }
}

// SSE 실패 시 Polling fallback — Redis 캐시 조회 (결과 없으면 204, 있으면 200)
public LoanEvaluationResultResponse getResult(String requestKey) {
    String json = redisTemplate.opsForValue().get(LOAN_RESULT_KEY_PREFIX + requestKey);
    return json == null ? null : objectMapper.readValue(json, LoanEvaluationResultResponse.class);
}
```
* **코드 링크**: [LoanService.java](https://github.com/Woori-Fisan/bankK/blob/develop/platform/backend/src/main/java/com/woorifisan/platform/domain/loan/service/LoanService.java) · [LoanController.java](https://github.com/Woori-Fisan/bankK/blob/develop/platform/backend/src/main/java/com/woorifisan/platform/domain/loan/controller/LoanController.java)

---
 
#### [기능 3] 동시성 제어 및 타행 이체 안전성 Saga 패턴 적용 - 비관적 락 + 보상 트랜잭션
 
* **기능 설명**: 출금·이체·대출 실행 시 `SELECT ... FOR UPDATE`로 계좌 락을 획득하고, 락 획득 후 잔액을 재검증(Double-Check)하여 동시 요청에 의한 마이너스 잔액을 원천 차단합니다. 타행 이체는 분산 트랜잭션 문제를 보상 트랜잭션 패턴으로 해결합니다. 출금 후 즉시 `PENDING` 응답을 반환하고, 별도 스레드에서 타행 서버에 입금을 요청합니다. 실패 시 최대 9회 재시도(1초 간격)를 수행하며, 최종 실패 시 출금을 환불하고 `FAILED`로 기록합니다. 클라이언트는 폴링 API로 결과를 확인합니다.
 * 이 구조는 Saga 패턴을 따릅니다. 출금·입금 각 단계를 독립 트랜잭션으로 분리하여 즉시 커밋함으로써 DB 락 점유 시간을 최소화하고, 실패 지점에서만 보상 트랜잭션(환불)을 실행하여 글로벌 트랜잭션 없이 최종 일관성을 보장합니다.


* **핵심 코드**:
```java
// bank/backend — 비관적 락 + Double-Check (TransferTxService)
// 1차 잔액 검증 후 FOR UPDATE 락 획득 → 재검증(Double-Check)
Account sender = accountMapper.findByAccountNoHash(cryptoUtil.hash(accountNo)).orElseThrow(...);
if (sender.getBalance().compareTo(request.getAmount()) < 0)
    throw new BusinessException(ErrorCode.INSUFFICIENT_BALANCE);
 
sender = accountMapper.findByIdForUpdate(sender.getId()).orElseThrow(...); // SELECT FOR UPDATE
if (sender.getBalance().compareTo(request.getAmount()) < 0)     // 락 후 재검증
    throw new BusinessException(ErrorCode.INSUFFICIENT_BALANCE);
accountMapper.updateBalance(sender.getId(), request.getAmount().negate());
 
// bank/backend — 보상 트랜잭션 (TransferCompensationService)
@Async("transferCompensationExecutor")
public void compensate(String depositBankCode, ..., String txId) {
    try {
        callExternalDeposit(depositBankCode, depositRequest, txId); // 타행 입금 시도
        transferTxService.updateLedgerStatus(txId, "SUCCESS");
    } catch (Exception depositEx) {
        // 입금 API 실패 → WebClient retryWhen 9회 상태 폴링
        boolean isProcessed = pollTransferStatus(depositBankCode, txId);
        if (!isProcessed) {
            transferTxService.refundTransfer(originalRequest, decryptedData, txId); // 환불
        }
        safeMarkLedgerAsFailed(txId);
    }
}
```
* **코드 링크**: [TransferTxService.java](https://github.com/Woori-Fisan/bankK/blob/develop/bank/backend/src/main/java/com/woorifisan/bank/domain/account/service/TransferTxService.java) · [TransferCompensationService.java](https://github.com/Woori-Fisan/bankK/blob/develop/bank/backend/src/main/java/com/woorifisan/bank/domain/account/service/TransferCompensationService.java)
---
 
#### [기능 4] Redis 기반 멱등성 보장 — 중복 거래 원천 차단
 
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
* **코드 링크**: [IdempotencyFilter.java](https://github.com/Woori-Fisan/bankK/blob/develop/platform/backend/src/main/java/com/woorifisan/platform/global/idempotency/filter/IdempotencyFilter.java)

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
* **코드 링크**: [ControllerLoggingAspect.java](https://github.com/Woori-Fisan/bankK/blob/develop/platform/backend/src/main/java/com/woorifisan/platform/global/aop/aspect/ControllerLoggingAspect.java)

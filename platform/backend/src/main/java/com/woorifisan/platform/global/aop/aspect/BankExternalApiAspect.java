package com.woorifisan.platform.global.aop.aspect;

import static net.logstash.logback.argument.StructuredArguments.entries;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.woorifisan.platform.global.config.BankNetworkConfig;
import com.woorifisan.platform.global.config.BankNetworkConfig.BankProperty;
import com.woorifisan.platform.global.exception.BankCoreException;
import com.woorifisan.platform.global.exception.BusinessException;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

/**
 * 은행 코어 서버로의 외부 API 호출을 로깅하는 AOP Aspect.
 *
 * <p>{@link com.woorifisan.platform.domain.bank.external.client.BankExternalClient}의
 * 모든 메서드에 적용되며, 요청 · 응답 · 예외를 구조화된 JSON 로그로 기록한다.</p>
 */
@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class BankExternalApiAspect {

    private final ObjectMapper      objectMapper;
    private final BankNetworkConfig bankNetworkConfig;

    private static final String MDC_BANK_CODE = "bankCode";
    private static final String MDC_API_TYPE  = "apiType";

    /** BankExternalClient 메서드명 → BankNetworkConfig URL 키 매핑 */
    private static final Map<String, String> API_TYPE_TO_URL_KEY = Map.of(
            "fetchRecipient",        "recipient",
            "executeTransfer",       "transfer",
            "fetchTransferWithdraw", "withdraw",
            "fetchDeposit",          "deposit",
            "fetchBalance",          "balance",
            "withdraw",              "withdraw",
            "fetchHistory",          "history"
    );

    /** {@link com.woorifisan.platform.domain.bank.external.client.BankExternalClient}의 모든 public 메서드를 포인트컷으로 지정한다. */
    @Pointcut("execution(* com.woorifisan.platform.domain.bank.external.client.BankExternalClient.*(..))")
    public void bankExternalClientPointcut() {}

    /**
     * 은행 코어 API 호출의 요청 · 응답 · 예외를 Around Advice로 로깅한다.
     * <ul>
     *   <li>정상 종료 : INFO — 소요 시간 · 응답 결과 포함</li>
     *   <li>BusinessException : WARN — 예상된 비즈니스 오류 (errorCode 포함)</li>
     *   <li>그 외 Exception : ERROR — 예상치 못한 시스템 오류 (stacktrace 포함)</li>
     * </ul>
     */
    @Around("bankExternalClientPointcut()")
    public Object logBankExternalApi(ProceedingJoinPoint joinPoint) throws Throwable {
        Object[] args    = joinPoint.getArgs();
        String apiType   = joinPoint.getSignature().getName();
        String bankCode  = args.length > 0 ? String.valueOf(args[0]) : "UNKNOWN";
        String requestJson = args.length > 1 ? serialize(args[1]) : "{}";

        // MDC 적재 — 이후 출력되는 모든 로그에 bankCode, apiType 자동 포함
        MDC.put(MDC_BANK_CODE, bankCode);
        MDC.put(MDC_API_TYPE, apiType);

        // 은행 코어 URL 조회 — apiType(메서드명) → URL 키 → 실제 엔드포인트
        BankProperty bankProperty = bankNetworkConfig.getBankProperty(bankCode);
        String urlKey = API_TYPE_TO_URL_KEY.getOrDefault(apiType, apiType);
        String bankUri = bankProperty != null ? bankProperty.getUrl(urlKey) : null;

        Map<String, Object> bankContext = new HashMap<>();
        bankContext.put("bankKeyId", null); // 추후 구현 예정
        bankContext.put("bankCode", bankCode);
        bankContext.put("apiType", apiType);
        bankContext.put("httpMethod", "POST"); // BankExternalClient는 모든 요청을 POST로 전송
        bankContext.put("httpUri", bankUri);
        bankContext.put("request", requestJson);

        bankContext.put("logType", "BANK_REQ");
        log.info("[BankAPI][Request] {}", apiType, entries(Map.of("http", bankContext)));

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        try {
            Object result = joinPoint.proceed();
            stopWatch.stop();

            bankContext.put("elapsedMs", stopWatch.getTotalTimeMillis());
            bankContext.put("httpStatus", 200); // onStatus 에러 핸들러 미발동 = 2xx 성공
            bankContext.put("response", serialize(result));
            bankContext.put("logType", "BANK_RES");
            log.info("[BankAPI][Response] {}", apiType, entries(Map.of("http", bankContext)));

            return result;
        } catch (BankCoreException e) {
            // 은행 코어 에러 — 우리 플랫폼 코드 + 은행 원본 코드/메시지 + HTTP 상태 함께 기록
            stopWatch.stop();

            bankContext.put("elapsedMs", stopWatch.getTotalTimeMillis());
            bankContext.put("httpStatus", e.getBankHttpStatus());
            bankContext.put("errorCode", e.getBankErrorCode());
            bankContext.put("errorMessage", e.getBankErrorMessage());
            bankContext.put("logType", "BANK_ERR");
            log.warn("[BankAPI][BusinessError] {}", apiType, entries(Map.of("http", bankContext)));

            throw e;
        } catch (BusinessException e) {
            // 그 외 비즈니스 예외 (은행 코어 이외 경로에서 발생한 BusinessException)
            stopWatch.stop();

            bankContext.put("elapsedMs", stopWatch.getTotalTimeMillis());
            bankContext.put("httpStatus", e.getErrorCode().getHttpStatus().value());
            bankContext.put("errorCode", e.getErrorCode().getCode());
            bankContext.put("errorMessage", e.getMessage());
            bankContext.put("logType", "BANK_ERR");
            log.warn("[BankAPI][BusinessError] {}", apiType, entries(Map.of("http", bankContext)));

            throw e;
        } catch (Exception e) {
            stopWatch.stop();

            bankContext.put("elapsedMs", stopWatch.getTotalTimeMillis());
            bankContext.put("exceptionType", e.getClass().getSimpleName());
            bankContext.put("errorMessage", e.getMessage());
            // BANK_COMM_ERR은 DB 저장 대상 제외 — logType 미설정으로 Fluent Bit 필터 통과 안 함
            log.error("[BankAPI][SystemError] {}", apiType, entries(Map.of("http", bankContext)), e);

            throw e;
        } finally {
            MDC.remove(MDC_BANK_CODE);
            MDC.remove(MDC_API_TYPE);
        }
    }

    /** 객체를 JSON 문자열로 직렬화한다. 직렬화 실패 시 {@code toString()}으로 폴백한다. */
    private String serialize(Object obj) {
        if (obj == null) return "null";
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return String.valueOf(obj);
        }
    }
}
package com.woorifisan.platform.global.aop.aspect;

import static net.logstash.logback.argument.StructuredArguments.entries;

import com.fasterxml.jackson.databind.ObjectMapper;
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

    private final ObjectMapper objectMapper;

    private static final String MDC_BANK_CODE = "bankCode";
    private static final String MDC_API_TYPE  = "apiType";

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

        Map<String, Object> bankContext = new HashMap<>();
        bankContext.put("bankCode", bankCode);
        bankContext.put("apiType", apiType);
        bankContext.put("request", requestJson);

        log.info("[BankAPI][Request] {}", apiType, entries(Map.of("bank", bankContext)));

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        try {
            Object result = joinPoint.proceed();
            stopWatch.stop();

            bankContext.put("elapsedMs", stopWatch.getTotalTimeMillis());
            bankContext.put("result", serialize(result));
            log.info("[BankAPI][Response] {}", apiType, entries(Map.of("bank", bankContext)));

            return result;
        } catch (BusinessException e) {
            stopWatch.stop();

            bankContext.put("elapsedMs", stopWatch.getTotalTimeMillis());
            bankContext.put("errorCode", e.getErrorCode().getCode());
            bankContext.put("errorMessage", e.getMessage());
            log.warn("[BankAPI][BusinessError] {}", apiType, entries(Map.of("bank", bankContext)));

            throw e;
        } catch (Exception e) {
            stopWatch.stop();

            bankContext.put("elapsedMs", stopWatch.getTotalTimeMillis());
            bankContext.put("exceptionType", e.getClass().getSimpleName());
            bankContext.put("errorMessage", e.getMessage());
            log.error("[BankAPI][SystemError] {}", apiType, entries(Map.of("bank", bankContext)), e);

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
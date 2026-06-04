package com.woorifisan.platform.global.aop.aspect;

import static net.logstash.logback.argument.StructuredArguments.entries;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.woorifisan.platform.global.exception.BusinessException;
import com.woorifisan.platform.global.response.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class ControllerLoggingAspect {

    private final ObjectMapper objectMapper;

    // Client IP 추출 헤더 우선순위
    private static final List<String> IP_HEADER_CANDIDATES = List.of(
            "X-Forwarded-For", "Proxy-Client-IP", "WL-Proxy-Client-IP"
    );
    private static final String UNKNOWN_IP = "unknown";

    // IPv6 로컬호스트 정규화
    private static final String IPV6_LOCALHOST       = "0:0:0:0:0:0:0:1";
    private static final String IPV6_SHORT_LOCALHOST = "::1";
    private static final String LOCALHOST            = "127.0.0.1";

    // 직렬화 제외 타입 (서블릿 내부 객체 · 멀티파트)
    // Jackson으로 직렬화하려고 시도하면 순환 참조 오류가 발생하거나 직렬화 실패 예외가 발생할 수 있음
    private static final Set<Class<?>> NON_SERIALIZABLE_TYPES = Set.of(
            HttpServletRequest.class,
            HttpServletResponse.class,
            MultipartFile.class,
            org.springframework.validation.Errors.class,       // BindingResult 포함
            jakarta.servlet.http.HttpSession.class,
            java.security.Principal.class,
            org.springframework.web.context.request.WebRequest.class
    );

    @Pointcut("@within(org.springframework.web.bind.annotation.RestController)")
    public void controllerPointcut() {}

    @Around("controllerPointcut()")
    public Object logController(ProceedingJoinPoint joinPoint) throws Throwable {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest  request  = attributes.getRequest();
        HttpServletResponse response = attributes.getResponse();

        Object[] args      = joinPoint.getArgs();
        String   argsJson  = serialize(args);
        String   className = joinPoint.getSignature().getDeclaringType().getSimpleName();
        String   methodName = joinPoint.getSignature().getName();

        Map<String, Object> httpContext = new HashMap<>();
        httpContext.put("bankKeyId", null); // 추후 구현 예정
        httpContext.put("httpMethod", request.getMethod());
        httpContext.put("httpUri", request.getRequestURI());
        httpContext.put("clientIp", getClientIp(request));
        httpContext.put("controller", className + "." + methodName);
        httpContext.put("request", argsJson);
        putBankCodes(args, httpContext);

        httpContext.put("logType", "CONTROLLER_REQ");
        log.info("[Request] {}", className + "." + methodName, entries(Map.of("http", httpContext)));

        long start = System.currentTimeMillis();
        try {
            Object result        = joinPoint.proceed();
            long   executionTime = System.currentTimeMillis() - start;

            applyElapsedAndStatus(httpContext, executionTime, response);
            httpContext.put("response", serialize(result));
            httpContext.put("logType", "CONTROLLER_RES");
            log.info("[Response] {}", className + "." + methodName, entries(Map.of("http", httpContext)));
            return result;
        } catch (BusinessException e) {
            long executionTime = System.currentTimeMillis() - start;
            applyElapsedAndStatus(httpContext, executionTime, e.getErrorCode().getHttpStatus().value());
            httpContext.put("exception", e.getClass().getSimpleName());
            httpContext.put("errorCode", e.getErrorCode().getCode());
            httpContext.put("errorMessage", e.getMessage());
            httpContext.put("logType", "CONTROLLER_ERR");
            log.warn("[Error] {}", className + "." + methodName, entries(Map.of("http", httpContext)));
            throw e;
        } catch (Throwable e) {
            long executionTime = System.currentTimeMillis() - start;
            applyElapsedAndStatus(httpContext, executionTime, HttpStatus.INTERNAL_SERVER_ERROR.value());
            httpContext.put("exception", e.getClass().getSimpleName());
            httpContext.put("errorCode", ErrorCode.INTERNAL_SERVER_ERROR.getCode());
            httpContext.put("errorMessage", e.getMessage());
            httpContext.put("logType", "CONTROLLER_ERR");
            log.error("[Error] {}", className + "." + methodName, entries(Map.of("http", httpContext)));
            throw e;
        }
    }

    /** 정상 응답 로그용 — HttpServletResponse에서 status를 읽어 httpContext에 추가한다. */
    private void applyElapsedAndStatus(Map<String, Object> httpContext, long executionTime, HttpServletResponse response) {
        httpContext.put("elapsedMs", executionTime);
        if (response != null) {
            httpContext.put("httpStatus", response.getStatus());
        }
    }

    /** 예외 로그용 — status를 호출자가 직접 결정해 httpContext에 추가한다. (response 미작성 시점 대응) */
    private void applyElapsedAndStatus(Map<String, Object> httpContext, long executionTime, int status) {
        httpContext.put("elapsedMs", executionTime);
        httpContext.put("httpStatus", status);
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = IP_HEADER_CANDIDATES.stream()
                .map(request::getHeader)
                .filter(h -> h != null && !h.isBlank() && !UNKNOWN_IP.equalsIgnoreCase(h))
                .findFirst()
                .orElse(request.getRemoteAddr());
        return normalizeIp(ip);
    }

    private String normalizeIp(String ip) {
        if (IPV6_LOCALHOST.equals(ip) || IPV6_SHORT_LOCALHOST.equals(ip)) {
            return LOCALHOST;
        }
        return ip;
    }

    // bankCode(출금/단일) → httpContext["bankCode"], depositBankCode → httpContext["targetCode"]
    // getBankCode() 우선, 없으면 withdrawal*/source* → bankCode, deposit*/target* → targetCode 로 분류
    private void putBankCodes(Object[] args, Map<String, Object> httpContext) {
        for (Object arg : args) {
            if (arg == null) continue;
            if (NON_SERIALIZABLE_TYPES.stream().anyMatch(t -> t.isInstance(arg))) continue;
            try {
                java.lang.reflect.Method getter = arg.getClass().getMethod("getBankCode");
                Object value = getter.invoke(arg);
                if (value instanceof String s && !s.isBlank()) {
                    httpContext.put("bankCode", s);
                    return;
                }
            } catch (NoSuchMethodException ignored) {
                String bankCode   = null;
                String targetCode = null;
                for (java.lang.reflect.Field field : arg.getClass().getDeclaredFields()) {
                    String name = field.getName();
                    if (!name.endsWith("BankCode")) continue;
                    try {
                        String getterName = "get" + Character.toUpperCase(name.charAt(0)) + name.substring(1);
                        Object value = arg.getClass().getMethod(getterName).invoke(arg);
                        if (!(value instanceof String s) || s.isBlank()) continue;
                        if (name.startsWith("withdrawal") || name.startsWith("source")) {
                            bankCode = s;
                        } else if (name.startsWith("deposit") || name.startsWith("target")) {
                            targetCode = s;
                        } else if (bankCode == null) {
                            bankCode = s;
                        }
                    } catch (Exception ignored2) {}
                }
                // withdrawalBankCode 없이 depositBankCode만 있으면 bankCode로 승격
                if (bankCode == null && targetCode != null) { bankCode = targetCode; targetCode = null; }
                if (bankCode   != null) httpContext.put("bankCode",   bankCode);
                if (targetCode != null) httpContext.put("targetCode", targetCode);
                if (bankCode != null || targetCode != null) return;
            } catch (Exception ignored) {}
        }
    }

    private String serialize(Object obj) {
        if (obj == null) return "null";
        if (obj instanceof Object[] args) {
            try {
                List<String> elements = Arrays.stream(args)
                        .filter(arg -> arg == null ||
                                NON_SERIALIZABLE_TYPES.stream().noneMatch(t -> t.isInstance(arg)))
                        .map(this::serialize)
                        .toList();
                if (elements.isEmpty()) return null;
                if (elements.size() == 1) return elements.get(0);
                return elements.stream().collect(Collectors.joining(", ", "[", "]"));
            } catch (Throwable t) {
                return String.valueOf(args);
            }
        }
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Throwable t) {
            return String.valueOf(obj);
        }
    }
}
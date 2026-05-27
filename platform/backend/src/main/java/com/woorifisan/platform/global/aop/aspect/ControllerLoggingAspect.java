package com.woorifisan.platform.global.aop.aspect;

import static net.logstash.logback.argument.StructuredArguments.entries;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.woorifisan.platform.global.exception.BusinessException;
import com.woorifisan.platform.global.response.ApiResponse;
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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

/**
 * {@link org.springframework.web.bind.annotation.RestController} 메서드의
 * 요청 · 응답 · 예외를 구조화된 JSON 로그로 기록하는 AOP Aspect.
 *
 * <p>요청 진입 시 staffId를 MDC에 적재하고, 처리 완료 후 반드시 제거한다.</p>
 */
@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class ControllerLoggingAspect {

    private final ObjectMapper objectMapper;

    private static final String MDC_STAFF_ID = "staffId";

    // Client IP 추출 헤더 우선순위
    private static final List<String> IP_HEADER_CANDIDATES = List.of(
            "X-Forwarded-For", "Proxy-Client-IP", "WL-Proxy-Client-IP"
    );
    private static final String UNKNOWN_IP = "unknown";

    // IPv6 로컬호스트 정규화
    private static final String IPV6_LOCALHOST      = "0:0:0:0:0:0:0:1";
    private static final String IPV6_SHORT_LOCALHOST = "::1";
    private static final String LOCALHOST           = "127.0.0.1";

    // 직렬화 제외 타입 (서블릿 내부 객체 · 멀티파트)
    private static final Set<Class<?>> NON_SERIALIZABLE_TYPES = Set.of(
            HttpServletRequest.class, HttpServletResponse.class, MultipartFile.class
    );

    /** 모든 {@code @RestController} 클래스의 메서드를 포인트컷으로 지정한다. */
    @Pointcut("@within(org.springframework.web.bind.annotation.RestController)")
    public void controllerPointcut() {}

    /**
     * 요청 진입부터 응답(또는 예외) 반환까지 Around Advice로 로깅한다.
     * HTTP 컨텍스트가 없는 경우(Non-HTTP 호출)는 별도 포맷으로 기록한다.
     */
    @Around("controllerPointcut()")
    public Object logController(ProceedingJoinPoint joinPoint) throws Throwable {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        
        String className = joinPoint.getSignature().getDeclaringType().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        Object[] args = joinPoint.getArgs();
        String argsJson = serialize(args);

        // staffId 추출 및 MDC 적재
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String currentStaffId = null;
        if (auth != null) {
            Object principal = auth.getPrincipal();
            if (principal instanceof Long) {
                currentStaffId = String.valueOf(principal);
            } else if (principal instanceof String) {
                currentStaffId = (String) principal;
            }
        }

        if (currentStaffId != null) {
            MDC.put(MDC_STAFF_ID, currentStaffId);
        }

        // request / response 를 상단에서 한 번만 추출
        HttpServletRequest request = attributes != null ? attributes.getRequest() : null;
        HttpServletResponse response = attributes != null ? attributes.getResponse() : null;

        Map<String, Object> httpContext = new HashMap<>();
        httpContext.put("bankKeyId", null); // 추후 구현 예정
        if (request != null) {
            httpContext.put("method", request.getMethod());
            httpContext.put("uri", request.getRequestURI());
            httpContext.put("clientIp", getClientIp(request));
            httpContext.put("controller", className + "." + methodName);
            // 요청 시점에 bankCode를 한 번만 파싱해 httpContext에 저장
            // → 이후 RES / ERR 로그에서도 동일한 맵을 재사용하므로 자동으로 포함됨
            extractBankCode(args).ifPresent(code -> httpContext.put("bankCode", code));

            log.info("[Request] Args: {}", argsJson, entries(Map.of("http", httpContext)));
        } else {
            log.info("[Request] Non-HTTP | Controller: {}.{} | Args: {}",
                    className, methodName, argsJson);
        }

        long start = System.currentTimeMillis();
        try {
            Object result = joinPoint.proceed();
            long executionTime = System.currentTimeMillis() - start;

            if (request != null) {
                applyElapsedAndStatus(httpContext, executionTime, response);
                String resultJson = serialize(result);
                log.info("[Response] Result: {}", resultJson, entries(Map.of("http", httpContext)));
            } else {
                log.info("[Response] Non-HTTP | Controller: {}.{} | Time: {}ms | Result: {}",
                        className, methodName, executionTime, serialize(result));
            }
            return result;
        } catch (BusinessException e) {
            // 예상된 비즈니스 예외 — errorCode에서 status 직접 추출, WARN 레벨로 기록
            long executionTime = System.currentTimeMillis() - start;
            int httpStatus = e.getErrorCode().getHttpStatus().value();

            if (request != null) {
                applyElapsedAndStatus(httpContext, executionTime, httpStatus);
                httpContext.put("exception", e.getClass().getSimpleName());
                // GlobalExceptionHandler가 반환할 응답 바디를 재현하여 result로 기록
                String resultJson = serialize(ApiResponse.error(e.getErrorCode(), e.getMessage()));
                log.warn("[Error] Result: {} | Exception: {} | Message: {}",
                        resultJson, e.getClass().getSimpleName(), e.getMessage(), entries(Map.of("http", httpContext)));
            } else {
                log.warn("[Error] Non-HTTP | Controller: {}.{} | Time: {}ms | Exception: {} | Message: {}",
                        className, methodName, executionTime, e.getClass().getSimpleName(), e.getMessage());
            }
            throw e;
        } catch (Throwable e) {
            // 예상치 못한 시스템 예외 — status 500, ERROR 레벨로 기록
            long executionTime = System.currentTimeMillis() - start;

            if (request != null) {
                applyElapsedAndStatus(httpContext, executionTime, HttpStatus.INTERNAL_SERVER_ERROR.value());
                httpContext.put("exception", e.getClass().getSimpleName());
                // 시스템 예외는 GlobalExceptionHandler가 INTERNAL_SERVER_ERROR로 응답
                String resultJson = serialize(ApiResponse.error(ErrorCode.INTERNAL_SERVER_ERROR));
                log.error("[Error] Result: {} | Exception: {} | Message: {}",
                        resultJson, e.getClass().getSimpleName(), e.getMessage(), entries(Map.of("http", httpContext)));
            } else {
                log.error("[Error] Non-HTTP | Controller: {}.{} | Time: {}ms | Exception: {} | Message: {}",
                        className, methodName, executionTime, e.getClass().getSimpleName(), e.getMessage());
            }
            throw e;
        } finally {
            MDC.remove(MDC_STAFF_ID);
        }
    }

    /** 정상 응답 로그용 — HttpServletResponse에서 status를 읽어 httpContext에 추가한다. */
    private void applyElapsedAndStatus(Map<String, Object> httpContext, long executionTime, HttpServletResponse response) {
        httpContext.put("elapsedMs", executionTime);
        if (response != null) {
            httpContext.put("status", response.getStatus());
        }
    }

    /** 예외 로그용 — status를 호출자가 직접 결정해 httpContext에 추가한다. (response 미작성 시점 대응) */
    private void applyElapsedAndStatus(Map<String, Object> httpContext, long executionTime, int status) {
        httpContext.put("elapsedMs", executionTime);
        httpContext.put("status", status);
    }

    /** 프록시 환경을 고려해 {@link #IP_HEADER_CANDIDATES} 우선순위대로 실제 클라이언트 IP를 추출한다. */
    private String getClientIp(HttpServletRequest request) {
        String ip = IP_HEADER_CANDIDATES.stream()
                .map(request::getHeader)
                .filter(h -> h != null && !h.isBlank() && !UNKNOWN_IP.equalsIgnoreCase(h))
                .findFirst()
                .orElse(request.getRemoteAddr());
        return normalizeIp(ip);
    }

    /** IPv6 루프백 주소를 IPv4 {@code 127.0.0.1}로 정규화한다. */
    private String normalizeIp(String ip) {
        if (IPV6_LOCALHOST.equals(ip) || IPV6_SHORT_LOCALHOST.equals(ip)) {
            return LOCALHOST;
        }
        return ip;
    }

    /**
     * 컨트롤러 파라미터 배열에서 {@code bankCode} 필드를 찾아 반환한다.
     *
     * <p>직렬화 없이 원본 객체에서 직접 읽으므로 JSON 파싱 비용이 없다.
     * {@code getBankCode()} 메서드가 있는 첫 번째 파라미터에서 추출한다.</p>
     */
    private java.util.Optional<String> extractBankCode(Object[] args) {
        for (Object arg : args) {
            if (arg == null) continue;
            if (NON_SERIALIZABLE_TYPES.stream().anyMatch(t -> t.isInstance(arg))) continue;
            try {
                java.lang.reflect.Method getter = arg.getClass().getMethod("getBankCode");
                Object value = getter.invoke(arg);
                if (value instanceof String s && !s.isBlank()) {
                    return java.util.Optional.of(s);
                }
            } catch (NoSuchMethodException ignored) {
                // 해당 파라미터에 getBankCode() 없음 — 다음 파라미터로
            } catch (Exception ignored) {}
        }
        return java.util.Optional.empty();
    }

    /** 서블릿 · 멀티파트 객체를 제외하고 파라미터를 JSON 문자열로 직렬화한다. 직렬화 실패 시 {@code toString()}으로 폴백한다. */
    private String serialize(Object obj) {
        if (obj == null) return "null";
        if (obj instanceof Object[] args) {
            return Arrays.stream(args)
                    .filter(arg -> arg == null ||
                            NON_SERIALIZABLE_TYPES.stream().noneMatch(t -> t.isInstance(arg)))
                    .map(this::serialize)
                    .collect(Collectors.joining(", ", "[", "]"));
        }
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return String.valueOf(obj);
        }
    }
}

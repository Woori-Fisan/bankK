package com.woorifisan.platform.global.aop.aspect;

import static net.logstash.logback.argument.StructuredArguments.entries;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class ControllerLoggingAspect {

    private final ObjectMapper objectMapper;
    private static final String MDC_STAFF_ID = "staffId";

    @Pointcut("@within(org.springframework.web.bind.annotation.RestController)")
    public void controllerPointcut() {}

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

        Map<String, Object> httpContext = new HashMap<>();
        httpContext.put("bankKeyId", null); // 추후 구현 예정
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            httpContext.put("method", request.getMethod());
            httpContext.put("uri", request.getRequestURI());
            httpContext.put("clientIp", getClientIp(request));
            httpContext.put("controller", className + "." + methodName);
            
            log.info("[Request] Args: {}", argsJson, entries(Map.of("http", httpContext)));
        } else {
            log.info("[Request] Non-HTTP | Controller: {}.{} | Args: {}",
                    className, methodName, argsJson);
        }

        long start = System.currentTimeMillis();
        try {
            Object result = joinPoint.proceed();
            long executionTime = System.currentTimeMillis() - start;
            
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                HttpServletResponse response = attributes.getResponse();
                
                httpContext.put("elapsedMs", executionTime);
                if (response != null) {
                    httpContext.put("status", response.getStatus());
                }
                
                String resultJson = serialize(result);
                log.info("[Response] Result: {}", resultJson, entries(Map.of("http", httpContext)));
            } else {
                log.info("[Response] Non-HTTP | Controller: {}.{} | Time: {}ms | Result: {}",
                        className, methodName, executionTime, serialize(result));
            }
            return result;
        } catch (Throwable e) {
            long executionTime = System.currentTimeMillis() - start;
            
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                HttpServletResponse response = attributes.getResponse();
                
                httpContext.put("elapsedMs", executionTime);
                if (response != null) {
                    httpContext.put("status", response.getStatus());
                }
                httpContext.put("exception", e.getClass().getSimpleName());

                log.error("[Error] Exception: {} | Message: {}",
                        e.getClass().getSimpleName(), e.getMessage(), entries(Map.of("http", httpContext)));
            } else {
                log.error("[Error] Non-HTTP | Controller: {}.{} | Time: {}ms | Exception: {} | Message: {}",
                        className, methodName, executionTime, e.getClass().getSimpleName(), e.getMessage());
            }
            throw e;
        } finally {
            MDC.remove(MDC_STAFF_ID);
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return normalizeIp(ip);
    }

    private String normalizeIp(String ip) {
        if ("0:0:0:0:0:0:0:1".equals(ip) || "::1".equals(ip)) {
            return "127.0.0.1";
        }
        return ip;
    }

    private String serialize(Object obj) {
        if (obj == null) return "null";
        if (obj instanceof Object[] args) {
            return Arrays.stream(args)
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

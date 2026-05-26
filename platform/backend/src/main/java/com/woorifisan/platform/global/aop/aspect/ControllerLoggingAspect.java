package com.woorifisan.platform.global.aop.aspect;

import jakarta.servlet.http.HttpServletRequest;
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

import java.util.Arrays;

@Aspect
@Component
@Slf4j
public class ControllerLoggingAspect {

    private static final String MDC_STAFF_ID = "staffId";

    @Pointcut("@within(org.springframework.web.bind.annotation.RestController)")
    public void controllerPointcut() {}

    @Around("controllerPointcut()")
    public Object logController(ProceedingJoinPoint joinPoint) throws Throwable {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        
        String className = joinPoint.getSignature().getDeclaringTypeName();
        String methodName = joinPoint.getSignature().getName();
        Object[] args = joinPoint.getArgs();

        // SecurityContext에서 staffId 추출하여 MDC에 적재
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Long) {
            MDC.put(MDC_STAFF_ID, String.valueOf(auth.getPrincipal()));
        }

        String staffId = MDC.get(MDC_STAFF_ID);
        String staffInfo = staffId != null ? "[StaffId: " + staffId + "] " : "";

        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            log.info("{} [Request] {} {} | Controller: {}.{} | Args: {}", 
                    staffInfo, request.getMethod(), request.getRequestURI(), className, methodName, Arrays.toString(args));
        } else {
            log.info("{} [Request] Non-HTTP | Controller: {}.{} | Args: {}", 
                    staffInfo, className, methodName, Arrays.toString(args));
        }

        long start = System.currentTimeMillis();
        try {
            Object result = joinPoint.proceed();
            long executionTime = System.currentTimeMillis() - start;
            
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                log.info("{} [Response] {} {} | Controller: {}.{} | Time: {}ms | Result: {}", 
                        staffInfo, request.getMethod(), request.getRequestURI(), className, methodName, executionTime, result);
            } else {
                log.info("{} [Response] Non-HTTP | Controller: {}.{} | Time: {}ms | Result: {}", 
                        staffInfo, className, methodName, executionTime, result);
            }
            return result;
        } catch (Throwable e) {
            long executionTime = System.currentTimeMillis() - start;
            
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                log.error("{} [Error] {} {} | Controller: {}.{} | Time: {}ms | Exception: {} | Message: {}", 
                        staffInfo, request.getMethod(), request.getRequestURI(), className, methodName, executionTime, e.getClass().getSimpleName(), e.getMessage());
            } else {
                log.error("{} [Error] Non-HTTP | Controller: {}.{} | Time: {}ms | Exception: {} | Message: {}", 
                        staffInfo, className, methodName, executionTime, e.getClass().getSimpleName(), e.getMessage());
            }
            throw e;
        } finally {
            MDC.remove(MDC_STAFF_ID);
        }
    }
}

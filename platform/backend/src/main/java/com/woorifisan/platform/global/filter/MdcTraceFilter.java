package com.woorifisan.platform.global.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 분산 추적을 위한 Trace ID 생성 및 MDC 관리 필터.
 * X-Request-ID 헤더가 있으면 재사용, 없으면 신규 생성.
 * 로그에 [traceId=...] 형식으로 포함되도록 지원함.
 */
@Component
public class MdcTraceFilter extends OncePerRequestFilter {

    private static final String TRACE_ID_HEADER = "X-Request-ID";
    private static final String MDC_TRACE_ID = "traceId";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        try {
            // 1. 헤더에서 Trace ID 추출 시도
            String traceId = Optional
                    .ofNullable(request.getHeader(TRACE_ID_HEADER))
                    .filter(StringUtils::hasText) // 2. 없으면 새로 생성
                    .orElse(UUID.randomUUID().toString().replace("-", ""));

            // 3. MDC에 적재 (logback-spring.xml에서 %X{traceId}로 참조)
            MDC.put(MDC_TRACE_ID, traceId);

            // 4. 클라이언트에게도 응답 헤더로 전달하여 트래킹 지원
            response.setHeader(TRACE_ID_HEADER, traceId);

            filterChain.doFilter(request, response);
        } finally {
            // 5. 요청 종료 후 반드시 비우기 (ThreadLocal 오염 방지)
            MDC.clear();
        }
    }
}

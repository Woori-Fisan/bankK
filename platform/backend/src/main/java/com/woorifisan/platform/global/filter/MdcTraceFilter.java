package com.woorifisan.platform.global.filter;

import com.woorifisan.platform.global.idempotency.filter.IdempotencyFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 분산 추적을 위한 Trace ID 생성 및 MDC 관리 필터.
 * 우선순위: X-Idempotency-Key > X-Request-ID > 신규 UUID
 * 멱등성 키가 있으면 그것을 traceId로 사용해 클라이언트~은행코어 로그를 같은 ID로 추적한다.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class MdcTraceFilter extends OncePerRequestFilter {

    private static final String TRACE_ID_HEADER    = "X-Request-ID";
    private static final String BANK_KEY_ID_HEADER  = "x-bank-key-id";
    private static final String MDC_TRACE_ID        = "traceId";
    private static final String MDC_BANK_KEY_ID     = "bankKeyId";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        try {
            // 1. X-Idempotency-Key 우선 → X-Request-ID → 신규 UUID
            String idempotencyKey = request.getHeader(IdempotencyFilter.IDEMPOTENCY_KEY_HEADER);
            String traceId = StringUtils.hasText(idempotencyKey)
                    ? idempotencyKey
                    : Optional.ofNullable(request.getHeader(TRACE_ID_HEADER))
                              .filter(StringUtils::hasText)
                              .orElse(UUID.randomUUID().toString().replace("-", ""));

            // 2. MDC에 적재 (logback-spring.xml에서 %X{traceId}로 참조)
            MDC.put(MDC_TRACE_ID, traceId);

            String bankKeyId = request.getHeader(BANK_KEY_ID_HEADER);
            if (StringUtils.hasText(bankKeyId)) {
                MDC.put(MDC_BANK_KEY_ID, bankKeyId);
            }

            // 3. 클라이언트 추적을 위해 응답 헤더로 echo
            response.setHeader(TRACE_ID_HEADER, traceId);

            filterChain.doFilter(request, response);
        } finally {
            // 4. 요청 종료 후 반드시 비우기 (ThreadLocal 오염 방지)
            MDC.clear();
        }
    }
}

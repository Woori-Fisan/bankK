package com.woorifisan.platform.global.idempotency.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

/**
 * 멱등성 키 기반 중복 요청 방지 필터.
 *
 * 클라이언트가 X-Idempotency-Key 헤더를 포함해 요청하면:
 * - 이미 처리된 요청(Redis에 캐시) → 저장된 응답을 그대로 반환, 거래 재처리 없음
 * - 처음 요청 → 처리 후 200 OK 응답을 Redis에 캐시 (TTL 60초)
 *
 * 적용 대상: 이체 / 출금 / 대출실행 (POST, 상태 변경 엔드포인트)
 * X-Idempotency-Key 헤더가 없는 요청은 그대로 통과시킨다.
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
@RequiredArgsConstructor
public class IdempotencyFilter extends OncePerRequestFilter {

    private static final String IDEMPOTENCY_KEY_HEADER = "X-Idempotency-Key";
    private static final String REPLAYED_HEADER = "X-Idempotency-Replayed";
    private static final String KEY_PREFIX = "idempotency:";
    private static final long TTL_SECONDS = 60L;

    // 멱등성 보장이 필요한 상태 변경 엔드포인트
    private static final Set<String> IDEMPOTENT_PATHS = Set.of(
            "/api/v1/bank/transfer",
            "/api/v1/bank/withdrawals",
            "/api/v1/loan/contract/execution"
    );

    private final RedisTemplate<String, String> redisTemplate;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // POST 메서드이면서 멱등성 대상 경로인 경우에만 필터 동작
        boolean isPost = HttpMethod.POST.name().equals(request.getMethod());
        boolean isTargetPath = IDEMPOTENT_PATHS.contains(request.getRequestURI());
        return !isPost || !isTargetPath;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String idempotencyKey = request.getHeader(IDEMPOTENCY_KEY_HEADER);

        // 멱등성 키 없으면 그냥 통과 (조회 등 헤더 없이 호출하는 경우 대비)
        if (!StringUtils.hasText(idempotencyKey)) {
            filterChain.doFilter(request, response);
            return;
        }

        String redisKey = KEY_PREFIX + idempotencyKey;
        String cached = redisTemplate.opsForValue().get(redisKey);

        if (cached != null) {
            // 이미 처리된 요청 — 캐시 응답 반환 (거래 재처리 없음)
            log.info("[멱등성] 중복 요청 감지 - key: {}, 캐시 응답 반환", idempotencyKey);
            response.setStatus(HttpServletResponse.SC_OK);
            response.setContentType("application/json;charset=UTF-8");
            response.setHeader(REPLAYED_HEADER, "true");
            response.getWriter().write(cached);
            return;
        }

        // 처음 요청 — 응답 본문을 캡처하기 위해 래퍼로 감쌈
        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);
        filterChain.doFilter(request, responseWrapper);

        // 200 OK 응답만 캐시 (오류 응답은 저장하지 않음 → 클라이언트가 재시도 가능해야 함)
        if (responseWrapper.getStatus() == HttpServletResponse.SC_OK) {
            String body = new String(responseWrapper.getContentAsByteArray(), StandardCharsets.UTF_8);
            redisTemplate.opsForValue().set(redisKey, body, TTL_SECONDS, TimeUnit.SECONDS);
            log.debug("[멱등성] 응답 캐시 저장 - key: {}", idempotencyKey);
        }

        // 실제 응답 본문을 클라이언트에게 전송 (래퍼가 버퍼에 가지고 있던 내용)
        responseWrapper.copyBodyToResponse();
    }
}

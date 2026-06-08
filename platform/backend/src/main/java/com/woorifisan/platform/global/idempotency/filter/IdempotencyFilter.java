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
 * X-Idempotency-Key 헤더 필수. 없으면 400 반환.
 *
 * 3단계 상태 머신으로 race condition을 방지한다:
 *   1) setIfAbsent(key, "PROCESSING") 원자적 선점
 *      - 선점 성공 → 처리 진행
 *      - 선점 실패 + 값이 "PROCESSING" → 409 (처리 중)
 *      - 선점 실패 + 값이 JSON body   → 200 재반환 (완료된 응답)
 *   2) 처리 성공(200 OK) → Redis 값을 실제 응답 body로 교체
 *   3) 처리 실패        → Redis 키 삭제 (클라이언트가 새 키로 재시도 가능)
 *
 * 적용 대상: 이체 / 출금 / 대출실행 (POST, 상태 변경 엔드포인트)
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
@RequiredArgsConstructor
public class IdempotencyFilter extends OncePerRequestFilter {

    public static final String IDEMPOTENCY_KEY_HEADER = "X-Idempotency-Key";
    private static final String REPLAYED_HEADER = "X-Idempotency-Replayed";
    private static final String KEY_PREFIX = "idempotency:";
    private static final String PROCESSING = "PROCESSING";
    private static final long TTL_SECONDS = 60L;

    // 멱등성 보장이 필요한 상태 변경 엔드포인트
    private static final Set<String> IDEMPOTENT_PATHS = Set.of(
            "/api/v1/bank/transfer",
            "/api/v1/bank/withdrawals",
            "/api/v1/loan/evaluation",
            "/api/v1/loan/contract/execution"
    );

    private final RedisTemplate<String, String> redisTemplate;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        boolean isPost = HttpMethod.POST.name().equals(request.getMethod());
        boolean isTargetPath = IDEMPOTENT_PATHS.contains(request.getServletPath());
        return !isPost || !isTargetPath;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String idempotencyKey = request.getHeader(IDEMPOTENCY_KEY_HEADER);

        // 금융 거래 POST에서 멱등성 키는 필수
        if (!StringUtils.hasText(idempotencyKey)) {
            writeError(response, HttpServletResponse.SC_BAD_REQUEST,
                    "IDEM_003", "X-Idempotency-Key 헤더가 필요합니다.");
            return;
        }

        String redisKey = KEY_PREFIX + idempotencyKey;

        // 1단계: 원자적으로 PROCESSING 상태 선점 시도
        Boolean acquired = redisTemplate.opsForValue()
                .setIfAbsent(redisKey, PROCESSING, TTL_SECONDS, TimeUnit.SECONDS);

        if (!Boolean.TRUE.equals(acquired)) {
            // 선점 실패 — 이미 처리 중이거나 완료된 요청
            String existing = redisTemplate.opsForValue().get(redisKey);

            if (PROCESSING.equals(existing) || existing == null) {
                // 처리 중인 요청 (또는 TTL 만료 직후 경합) → 409
                log.info("[멱등성] 처리 중 중복 요청 - key: {}", idempotencyKey);
                writeError(response, HttpServletResponse.SC_CONFLICT,
                        "IDEM_002", "동일 키의 요청이 처리 중입니다.");
                return;
            }

            // 완료된 요청 — 캐시된 응답 재반환
            log.info("[멱등성] 완료된 요청 재사용 - key: {}", idempotencyKey);
            response.setStatus(HttpServletResponse.SC_OK);
            response.setContentType("application/json;charset=UTF-8");
            response.setHeader(REPLAYED_HEADER, "true");
            response.getWriter().write(existing);
            return;
        }

        // 2단계: 처리권 획득 — 응답 본문을 캡처하기 위해 래퍼로 감쌈
        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);
        boolean completed = false;
        try {
            filterChain.doFilter(request, responseWrapper);
            completed = true;
        } finally {
            if (completed && responseWrapper.getStatus() == HttpServletResponse.SC_OK) {
                // 3단계 (성공): PROCESSING → 실제 응답 body로 교체
                String body = new String(responseWrapper.getContentAsByteArray(), StandardCharsets.UTF_8);
                redisTemplate.opsForValue().set(redisKey, body, TTL_SECONDS, TimeUnit.SECONDS);
                log.debug("[멱등성] 응답 캐시 저장 - key: {}", idempotencyKey);
            } else {
                // 3단계 (실패/예외): 선점 해제 — 클라이언트가 동일 키로 재시도 가능
                redisTemplate.delete(redisKey);
                log.info("[멱등성] 처리 실패 또는 예외, 키 해제 - key: {}, status: {}",
                        idempotencyKey, responseWrapper.getStatus());
            }
            responseWrapper.copyBodyToResponse();
        }
    }

    private void writeError(HttpServletResponse response, int status, String code, String message)
            throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(
                String.format("{\"success\":false,\"data\":null,\"error\":{\"code\":\"%s\",\"message\":\"%s\"}}",
                        code, message));
    }
}

package com.woorifisan.platform.global.idempotency.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletResponse;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

@ExtendWith(MockitoExtension.class)
class IdempotencyFilterTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOps;

    private IdempotencyFilter filter;

    private static final String TARGET_PATH = "/api/v1/bank/transfer";
    private static final String IDEM_KEY    = "test-idem-key-001";
    private static final String REDIS_KEY   = "idempotency:" + IDEM_KEY;

    @BeforeEach
    void setUp() {
        given(redisTemplate.opsForValue()).willReturn(valueOps);
        filter = new IdempotencyFilter(redisTemplate);
    }

    // ─────────────────────────────────────────────────────────────
    // 단일 스레드 시나리오
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("X-Idempotency-Key 헤더 누락 → 400 IDEM_003")
    void missingHeader_returns400() throws Exception {
        MockHttpServletRequest  request  = buildRequest(TARGET_PATH, null);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(400);
        assertThat(response.getContentAsString()).contains("IDEM_003");
    }

    @Test
    @DisplayName("첫 요청 정상 처리 → 200, 응답 body Redis에 캐시")
    void firstRequest_processesAndCachesBody() throws Exception {
        given(valueOps.setIfAbsent(eq(REDIS_KEY), eq("PROCESSING"), anyLong(), any()))
                .willReturn(true);

        MockHttpServletRequest  request  = buildRequest(TARGET_PATH, IDEM_KEY);
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = successChain("{\"success\":true,\"data\":{\"transactionId\":\"TX-001\"}}");

        filter.doFilterInternal(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
        // 성공 후 Redis에 캐시 저장 호출 확인
        verify(valueOps).set(eq(REDIS_KEY), any(String.class), anyLong(), any());
    }

    @Test
    @DisplayName("처리 중 동일 키 재요청 → 409 IDEM_002")
    void duplicateWhileProcessing_returns409() throws Exception {
        given(valueOps.setIfAbsent(eq(REDIS_KEY), eq("PROCESSING"), anyLong(), any()))
                .willReturn(false);
        given(valueOps.get(REDIS_KEY)).willReturn("PROCESSING");

        MockHttpServletRequest  request  = buildRequest(TARGET_PATH, IDEM_KEY);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(409);
        assertThat(response.getContentAsString()).contains("IDEM_002");
    }

    @Test
    @DisplayName("완료 후 동일 키 재요청 → 200, 캐시된 body 반환, X-Idempotency-Replayed: true")
    void duplicateAfterCompletion_returnsCachedResponse() throws Exception {
        String cachedBody = "{\"success\":true,\"data\":{\"transactionId\":\"TX-001\"}}";
        given(valueOps.setIfAbsent(eq(REDIS_KEY), eq("PROCESSING"), anyLong(), any()))
                .willReturn(false);
        given(valueOps.get(REDIS_KEY)).willReturn(cachedBody);

        MockHttpServletRequest  request  = buildRequest(TARGET_PATH, IDEM_KEY);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getHeader("X-Idempotency-Replayed")).isEqualTo("true");
        assertThat(response.getContentAsString()).isEqualTo(cachedBody);
    }

    @Test
    @DisplayName("처리 실패(500) → Redis 키 삭제 (클라이언트 재시도 허용)")
    void processingFailure_deletesRedisKey() throws Exception {
        given(valueOps.setIfAbsent(eq(REDIS_KEY), eq("PROCESSING"), anyLong(), any()))
                .willReturn(true);

        MockHttpServletRequest  request  = buildRequest(TARGET_PATH, IDEM_KEY);
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (req, res) -> ((HttpServletResponse) res).setStatus(500);

        filter.doFilterInternal(request, response, chain);

        verify(redisTemplate).delete(REDIS_KEY);
    }

    // ─────────────────────────────────────────────────────────────
    // 동시 요청 시나리오 (핵심)
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("스레드 2개가 동일 키로 동시 요청 → 정확히 1개만 200, 1개는 409")
    void concurrentSameKey_exactlyOneSuccessOneConflict() throws Exception {
        // Redis SET NX 원자성 시뮬레이션: 첫 호출만 true (두 스레드 중 한 쪽만 선점)
        AtomicInteger acquireCount = new AtomicInteger(0);
        given(valueOps.setIfAbsent(eq(REDIS_KEY), eq("PROCESSING"), anyLong(), any()))
                .willAnswer(inv -> acquireCount.getAndIncrement() == 0);
        given(valueOps.get(REDIS_KEY)).willReturn("PROCESSING");

        int threadCount = 2;
        CountDownLatch ready  = new CountDownLatch(threadCount); // 스레드 준비 완료 신호
        CountDownLatch start  = new CountDownLatch(1);            // 동시 출발 신호
        CountDownLatch done   = new CountDownLatch(threadCount); // 완료 신호
        int[] statuses = new int[threadCount];

        ExecutorService pool = Executors.newFixedThreadPool(threadCount);

        for (int i = 0; i < threadCount; i++) {
            final int idx = i;
            pool.submit(() -> {
                try {
                    ready.countDown();
                    start.await(); // 모든 스레드가 준비될 때까지 대기 후 동시 출발

                    MockHttpServletRequest  req = buildRequest(TARGET_PATH, IDEM_KEY);
                    MockHttpServletResponse res = new MockHttpServletResponse();
                    FilterChain chain = successChain("{\"success\":true}");

                    filter.doFilterInternal(req, res, chain);
                    statuses[idx] = res.getStatus();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                } finally {
                    done.countDown();
                }
            });
        }

        ready.await();          // 두 스레드 모두 준비될 때까지 대기
        start.countDown();      // 동시 출발
        assertThat(done.await(5, TimeUnit.SECONDS)).isTrue();
        pool.shutdown();

        assertThat(statuses).contains(200);
        assertThat(statuses).contains(409);
    }

    @Test
    @DisplayName("서로 다른 키를 가진 스레드 2개 → 둘 다 200")
    void concurrentDifferentKeys_bothSucceed() throws Exception {
        String keyA = "key-A";
        String keyB = "key-B";
        String redisKeyA = "idempotency:" + keyA;
        String redisKeyB = "idempotency:" + keyB;

        given(valueOps.setIfAbsent(eq(redisKeyA), eq("PROCESSING"), anyLong(), any()))
                .willReturn(true);
        given(valueOps.setIfAbsent(eq(redisKeyB), eq("PROCESSING"), anyLong(), any()))
                .willReturn(true);

        int threadCount = 2;
        CountDownLatch ready = new CountDownLatch(threadCount);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done  = new CountDownLatch(threadCount);
        String[] keys    = { keyA, keyB };
        int[]    statuses = new int[threadCount];

        ExecutorService pool = Executors.newFixedThreadPool(threadCount);

        for (int i = 0; i < threadCount; i++) {
            final int idx = i;
            pool.submit(() -> {
                try {
                    ready.countDown();
                    start.await();

                    MockHttpServletRequest  req = buildRequest(TARGET_PATH, keys[idx]);
                    MockHttpServletResponse res = new MockHttpServletResponse();
                    FilterChain chain = successChain("{\"success\":true}");

                    filter.doFilterInternal(req, res, chain);
                    statuses[idx] = res.getStatus();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                } finally {
                    done.countDown();
                }
            });
        }

        ready.await();
        start.countDown();
        assertThat(done.await(5, TimeUnit.SECONDS)).isTrue();
        pool.shutdown();

        assertThat(statuses[0]).isEqualTo(200);
        assertThat(statuses[1]).isEqualTo(200);
    }

    // ─────────────────────────────────────────────────────────────
    // 헬퍼
    // ─────────────────────────────────────────────────────────────

    private MockHttpServletRequest buildRequest(String path, String idempotencyKey) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", path);
        if (idempotencyKey != null) {
            request.addHeader(IdempotencyFilter.IDEMPOTENCY_KEY_HEADER, idempotencyKey);
        }
        return request;
    }

    /** 200 OK와 지정된 body를 기록하는 FilterChain 스텁 */
    private FilterChain successChain(String body) {
        return (req, res) -> {
            HttpServletResponse r = (HttpServletResponse) res;
            r.setStatus(200);
            r.setContentType("application/json;charset=UTF-8");
            r.getWriter().write(body);
        };
    }
}

package com.woorifisan.platform.global.lock;

import com.woorifisan.platform.global.exception.BusinessException;
import com.woorifisan.platform.global.response.ErrorCode;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

/**
 * Redis 기반 직원별 거래 세션 락.
 * 직원이 거래를 진행 중일 때 동일 직원의 새 거래 요청을 차단한다.
 *
 * Redis 키: tx:lock:{staffId}
 * 상태: PENDING (수신~은행호출 전) → PROCESSING (은행 코어 호출 중)
 * TTL: 30초 — 서버 크래시 시 자동 해제
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TxSessionLockService {

    private static final String LOCK_KEY_PREFIX = "tx:lock:";
    private static final long LOCK_TTL_SECONDS = 30L;

    private final RedisTemplate<String, String> redisTemplate;

    /**
     * PENDING 상태로 락 획득 시도.
     * 이미 락이 있으면 BusinessException(DUPLICATE_REQUEST) 발생.
     */
    public void acquireLock(Long staffId) {
        String key = LOCK_KEY_PREFIX + staffId;
        // SET NX EX — 원자적으로 키가 없을 때만 쓰기
        Boolean acquired = redisTemplate.opsForValue()
                .setIfAbsent(key, "PENDING", LOCK_TTL_SECONDS, TimeUnit.SECONDS);

        if (!Boolean.TRUE.equals(acquired)) {
            String currentStatus = redisTemplate.opsForValue().get(key);
            log.warn("[세션락] 거래 중복 감지 - staffId: {}, 현재상태: {}", staffId, currentStatus);
            throw new BusinessException(ErrorCode.DUPLICATE_REQUEST);
        }
        log.debug("[세션락] 획득 - staffId: {}", staffId);
    }

    /**
     * PENDING → PROCESSING 전환 (은행 코어 API 호출 직전에 호출).
     */
    public void upgradeToProcessing(Long staffId) {
        String key = LOCK_KEY_PREFIX + staffId;
        redisTemplate.opsForValue().set(key, "PROCESSING", LOCK_TTL_SECONDS, TimeUnit.SECONDS);
        log.debug("[세션락] PROCESSING 전환 - staffId: {}", staffId);
    }

    /**
     * 락 해제 (finally 블록에서 반드시 호출).
     */
    public void releaseLock(Long staffId) {
        redisTemplate.delete(LOCK_KEY_PREFIX + staffId);
        log.debug("[세션락] 해제 - staffId: {}", staffId);
    }
}

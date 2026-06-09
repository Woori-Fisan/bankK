package com.woorifisan.platform.global.util;

import java.time.Instant;
import java.util.concurrent.ThreadLocalRandom;
import org.slf4j.MDC;

/**
 * 비즈니스 로그 고유 ID 생성기.
 * 형식: {traceId}_{epochMillis}_{nanoOfSecond}_{rand4hex}
 * - 컨테이너 환경에서 OS 시계 해상도가 1ms로 제한될 경우 nanoOfSecond 하위 비트가
 *   항상 0이 되어 동일 밀리초 내 충돌이 발생할 수 있으므로 4자리 16진 난수를 suffix로 추가한다.
 * - traceId가 MDC에 없을 경우 "NOTRACE"로 대체한다.
 */
public final class LogIdGenerator {

    private static final String MDC_TRACE_ID = "traceId";
    private static final String NO_TRACE = "NOTRACE";

    private LogIdGenerator() {}

    public static String generate() {
        String traceId = MDC.get(MDC_TRACE_ID);
        if (traceId == null || traceId.isBlank()) {
            traceId = NO_TRACE;
        }
        Instant now  = Instant.now();
        int rand = ThreadLocalRandom.current().nextInt(0x10000); // 0x0000 ~ 0xFFFF
        return traceId + "_" + now.toEpochMilli() + "_" + now.getNano()
                + "_" + String.format("%04x", rand);
    }
}
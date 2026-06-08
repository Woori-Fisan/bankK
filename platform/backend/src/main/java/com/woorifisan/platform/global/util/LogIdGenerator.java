package com.woorifisan.platform.global.util;

import java.time.Instant;
import org.slf4j.MDC;

/**
 * 비즈니스 로그 고유 ID 생성기.
 * 형식: {traceId}_{epochMillis}_{nanoOfSecond}
 * traceId가 MDC에 없을 경우 "NOTRACE"로 대체한다.
 */
public final class LogIdGenerator {

    private static final String MDC_TRACE_ID  = "traceId";
    private static final String NO_TRACE      = "NOTRACE";

    private LogIdGenerator() {}

    public static String generate() {
        String traceId = MDC.get(MDC_TRACE_ID);
        if (traceId == null || traceId.isBlank()) {
            traceId = NO_TRACE;
        }
        Instant now = Instant.now();
        return traceId + "_" + now.toEpochMilli() + "_" + now.getNano();
    }
}
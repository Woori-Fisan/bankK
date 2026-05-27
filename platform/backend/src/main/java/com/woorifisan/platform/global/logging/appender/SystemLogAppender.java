package com.woorifisan.platform.global.logging.appender;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import com.woorifisan.platform.global.logging.SpringContextHolder;
import com.woorifisan.platform.global.logging.mapper.SystemLogMapper;
import com.woorifisan.platform.global.logging.model.SystemLog;
import java.lang.reflect.Field;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import net.logstash.logback.marker.MapEntriesAppendingMarker;
import org.slf4j.Marker;

/**
 * {@link com.woorifisan.platform.global.aop.aspect.ControllerLoggingAspect} /
 * {@link com.woorifisan.platform.global.aop.aspect.BankExternalApiAspect}의
 * 구조화 로그를 system_logs 테이블에 INSERT 하는 Logback Appender.
 *
 * <h3>동작 원리</h3>
 * <ol>
 *   <li>Spring ApplicationContext가 준비되기 전 호출되면 조용히 스킵한다.</li>
 *   <li>처리 대상 Logger(ControllerLoggingAspect · BankExternalApiAspect)가 아니면 무시한다.</li>
 *   <li>메시지 접두어([Request], [BankAPI][Request] 등)로 log_type을 결정한다.</li>
 *   <li>{@code entries(Map.of("http", httpContext))} 구조화 인자에서 리플렉션으로 컨텍스트 맵을 추출한다.</li>
 * </ol>
 *
 * <h3>logback-spring.xml 등록 예시</h3>
 * <pre>{@code
 * <appender name="DB_LOG"
 *           class="com.woorifisan.platform.global.logging.appender.SystemLogAppender"/>
 *
 * <appender name="ASYNC_DB_LOG" class="ch.qos.logback.classic.AsyncAppender">
 *     <appender-ref ref="DB_LOG"/>
 *     <queueSize>256</queueSize>
 *     <discardingThreshold>0</discardingThreshold>
 *     <maxFlushTime>2000</maxFlushTime>
 * </appender>
 * }</pre>
 */
public class SystemLogAppender extends AppenderBase<ILoggingEvent> {

    // ── 처리 대상 Logger 이름 suffix ──────────────────────────────────────
    private static final String CONTROLLER_LOGGER = "ControllerLoggingAspect";
    private static final String BANK_LOGGER       = "BankExternalApiAspect";

    // ── log_type 결정용 메시지 접두어 ─────────────────────────────────────
    private static final String MSG_CONTROLLER_REQ  = "[Request]";
    private static final String MSG_CONTROLLER_RES  = "[Response]";
    private static final String MSG_CONTROLLER_ERR  = "[Error]";
    private static final String MSG_BANK_REQ        = "[BankAPI][Request]";
    private static final String MSG_BANK_RES        = "[BankAPI][Response]";
    private static final String MSG_BANK_BIZ_ERR    = "[BankAPI][BusinessError]";
    private static final String MSG_BANK_SYS_ERR    = "[BankAPI][SystemError]";

    // ── bank_code NOT NULL 기본값 (controller 레벨에서는 bankCode 없음) ────
    private static final String UNKNOWN_BANK = "UNKNOWN";

    /**
     * MapEntriesAppendingMarker.map 필드 — 리플렉션 비용 절감을 위해 클래스 로드 시 1회만 추출.
     * 접근 실패 시 null로 유지되며, extractNestedContext()에서 graceful fallback 처리.
     */
    private static final Field MAP_ENTRIES_FIELD;
    static {
        Field f = null;
        try {
            f = MapEntriesAppendingMarker.class.getDeclaredField("map");
            f.setAccessible(true);
        } catch (Exception ignored) {
            // 리플렉션 실패 시 컨텍스트 추출 불가 — append()에서 null 반환으로 처리
        }
        MAP_ENTRIES_FIELD = f;
    }

    /** lazy-init: Spring 준비 전 null, 이후 캐싱하여 재사용 */
    private volatile SystemLogMapper mapper;

    // ═══════════════════════════════════════════════════════════════════════
    // AppenderBase 구현
    // ═══════════════════════════════════════════════════════════════════════

    @Override
    protected void append(ILoggingEvent event) {
        SystemLogMapper m = resolveMapper();
        if (m == null) return; // Spring 미준비 — 스킵

        String  loggerName   = event.getLoggerName();
        boolean isController = loggerName.endsWith(CONTROLLER_LOGGER);
        boolean isBank       = loggerName.endsWith(BANK_LOGGER);
        if (!isController && !isBank) return;

        SystemLog systemLog = isController
                ? buildControllerLog(event)
                : buildBankLog(event);

        if (systemLog == null) return;

        try {
            m.insert(systemLog);
        } catch (Exception e) {
            addError("[SystemLogAppender] DB insert 실패", e);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // ControllerLoggingAspect 로그 빌더
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * ControllerLoggingAspect 로그 이벤트를 SystemLog로 변환한다.
     *
     * <p>메시지 접두어가 알 수 없는 형식이면 null을 반환하여 삽입을 스킵한다.</p>
     */
    private SystemLog buildControllerLog(ILoggingEvent event) {
        String logType = resolveControllerLogType(event.getMessage());
        if (logType == null) return null;

        Map<String, String> mdc         = event.getMDCPropertyMap();
        Map<String, Object> httpContext = extractNestedContext(event, "http");
        Object[]            args        = event.getArgumentArray();

        // 첫 번째 positional 인자 = argsJson(REQ) / resultJson(RES/ERR)
        String bodyData = (args != null && args.length > 0)
                ? String.valueOf(args[0]) : null;

        return SystemLog.builder()
                .createdAt(toLocalDateTime(event.getTimeStamp()))
                .level(event.getLevel().toString())
                .logType(logType)
                .traceId(mdc.get("traceId"))
                .staffId(mdc.get("staffId"))
                .bankCode(UNKNOWN_BANK)  // controller 단계에서 bank 정보 없음
                .httpMethod(getStr(httpContext, "method"))
                .httpUri(getStr(httpContext, "uri"))
                .httpStatus(getInt(httpContext, "status"))
                .elapsedMs(getInt(httpContext, "elapsedMs"))
                .clientIp(getStr(httpContext, "clientIp"))
                .bodyData(bodyData)
                .errorCode(getStr(httpContext, "exception"))  // 예외 클래스명
                .build();
    }

    private String resolveControllerLogType(String message) {
        if (message.startsWith(MSG_CONTROLLER_REQ)) return "CONTROLLER_REQ";
        if (message.startsWith(MSG_CONTROLLER_RES)) return "CONTROLLER_RES";
        if (message.startsWith(MSG_CONTROLLER_ERR)) return "CONTROLLER_ERR";
        return null; // Non-HTTP 분기([Request] Non-HTTP ...) 등 알 수 없는 형식
    }

    // ═══════════════════════════════════════════════════════════════════════
    // BankExternalApiAspect 로그 빌더
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * BankExternalApiAspect 로그 이벤트를 SystemLog로 변환한다.
     *
     * <p>BankCoreException이면 {@code bankErrorCode / bankErrorMessage},
     * BusinessException이면 {@code errorCode / errorMessage}가 bankContext에 담긴다.</p>
     */
    private SystemLog buildBankLog(ILoggingEvent event) {
        String logType = resolveBankLogType(event.getMessage());
        if (logType == null) return null;

        Map<String, String> mdc         = event.getMDCPropertyMap();
        Map<String, Object> bankContext = extractNestedContext(event, "bank");

        String bankCode = getStr(bankContext, "bankCode");

        // 에러 코드 — BankCoreException 우선, 그 다음 BusinessException
        String errorCode = firstNonNull(
                getStr(bankContext, "bankErrorCode"),
                getStr(bankContext, "errorCode")
        );
        String errorMessage = firstNonNull(
                getStr(bankContext, "bankErrorMessage"),
                getStr(bankContext, "errorMessage")
        );

        return SystemLog.builder()
                .createdAt(toLocalDateTime(event.getTimeStamp()))
                .level(event.getLevel().toString())
                .logType(logType)
                .traceId(mdc.get("traceId"))
                .staffId(mdc.get("staffId"))
                .bankCode(bankCode != null ? bankCode : UNKNOWN_BANK)
                .elapsedMs(getInt(bankContext, "elapsedMs"))
                .bodyData(getStr(bankContext, "request"))  // REQ: 요청 JSON, RES/ERR: null
                .errorCode(errorCode)
                .errorMessage(errorMessage)
                .build();
    }

    private String resolveBankLogType(String message) {
        if (message.startsWith(MSG_BANK_REQ))     return "BANK_REQ";
        if (message.startsWith(MSG_BANK_RES))     return "BANK_RES";
        if (message.startsWith(MSG_BANK_BIZ_ERR)) return "BANK_ERR";
        if (message.startsWith(MSG_BANK_SYS_ERR)) return "BANK_COMM_ERR";
        return null;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 구조화 인자 추출 — MapEntriesAppendingMarker 리플렉션
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * {@code entries(Map.of("http", httpContext))} 형식의 구조화 인자에서
     * {@code contextKey}에 해당하는 중첩 맵을 추출한다.
     *
     * <p>탐색 순서:</p>
     * <ol>
     *   <li>{@code event.getArgumentArray()} — SLF4J 1.x / logstash 인자 전달 방식</li>
     *   <li>{@code event.getMarkerList()} — SLF4J 2.x fluent API 폴백</li>
     * </ol>
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> extractNestedContext(ILoggingEvent event, String contextKey) {
        // 1. argumentArray 탐색
        Object[] args = event.getArgumentArray();
        if (args != null) {
            for (Object arg : args) {
                Map<String, Object> result = tryExtractFromMarker(arg, contextKey);
                if (result != null) return result;
            }
        }

        // 2. SLF4J 2.x markerList 폴백
        List<Marker> markers = event.getMarkerList();
        if (markers != null) {
            for (Marker marker : markers) {
                Map<String, Object> result = tryExtractFromMarker(marker, contextKey);
                if (result != null) return result;
            }
        }

        return Map.of();
    }

    /**
     * 인자가 {@link MapEntriesAppendingMarker}이면 리플렉션으로 내부 맵을 꺼내
     * {@code contextKey}에 해당하는 값을 반환한다.
     * 추출 실패 또는 타입 불일치 시 {@code null}을 반환한다.
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> tryExtractFromMarker(Object arg, String contextKey) {
        if (!(arg instanceof MapEntriesAppendingMarker) || MAP_ENTRIES_FIELD == null) return null;
        try {
            Map<String, Object> outerMap = (Map<String, Object>) MAP_ENTRIES_FIELD.get(arg);
            Object nested = outerMap.get(contextKey);
            if (nested instanceof Map<?, ?> nestedMap) {
                return (Map<String, Object>) nestedMap;
            }
        } catch (Exception ignored) {}
        return null;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 공통 유틸
    // ═══════════════════════════════════════════════════════════════════════

    private String getStr(Map<String, Object> map, String key) {
        if (map == null || map.isEmpty()) return null;
        Object val = map.get(key);
        return val != null ? String.valueOf(val) : null;
    }

    private Integer getInt(Map<String, Object> map, String key) {
        if (map == null || map.isEmpty()) return null;
        Object val = map.get(key);
        if (val == null)             return null;
        if (val instanceof Integer i) return i;
        if (val instanceof Number n)  return n.intValue();
        try { return Integer.parseInt(String.valueOf(val)); }
        catch (NumberFormatException e) { return null; }
    }

    private LocalDateTime toLocalDateTime(long timestampMs) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(timestampMs), ZoneId.systemDefault());
    }

    private String firstNonNull(String... values) {
        for (String v : values) {
            if (v != null) return v;
        }
        return null;
    }

    /** Spring Bean 조회. 미준비 시 null 반환. */
    private SystemLogMapper resolveMapper() {
        if (mapper == null) {
            mapper = SpringContextHolder.getBean(SystemLogMapper.class);
        }
        return mapper;
    }
}

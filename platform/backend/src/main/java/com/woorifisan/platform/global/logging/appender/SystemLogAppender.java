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

// ControllerLoggingAspect / BankExternalApiAspect의 구조화 로그를 system_logs 테이블에 INSERT하는 Logback Appender.
// Spring 미준비 시 스킵, 대상 Logger 외 무시, 메시지 접두어로 log_type 결정, 리플렉션으로 컨텍스트 맵 추출.
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
    // [BankAPI][SystemError] → 파일 로그에만 기록, DB(system_logs) 저장 대상 제외

    // ── bank_code NOT NULL 기본값 (controller 레벨에서는 bankCode 없음) ────
    private static final String UNKNOWN_BANK = "UNKNOWN";

    // 리플렉션 비용 절감을 위해 클래스 로드 시 1회 추출. 실패 시 null 유지.
    private static final Field MAP_ENTRIES_FIELD;
    static {
        Field f = null;
        try {
            f = MapEntriesAppendingMarker.class.getDeclaredField("map");
            f.setAccessible(true);
        } catch (Exception ignored) {}
        MAP_ENTRIES_FIELD = f;
    }

    // Spring 준비 전 null, 준비 후 캐싱하여 재사용
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

    private SystemLog buildControllerLog(ILoggingEvent event) {
        String logType = resolveControllerLogType(event.getMessage());
        if (logType == null) return null;

        Map<String, String> mdc        = event.getMDCPropertyMap();
        Map<String, Object> httpContext = extractNestedContext(event, "http");

        String bodyData = switch (logType) {
            case "CONTROLLER_REQ" -> getStr(httpContext, "request");
            case "CONTROLLER_RES" -> getStr(httpContext, "response");
            default               -> null;
        };

        return SystemLog.builder()
                .createdAt(toLocalDateTime(event.getTimeStamp()))
                .level(event.getLevel().toString())
                .logType(logType)
                .traceId(mdc.get("traceId"))
                .staffId(mdc.get("staffId"))
                .bankCode(firstNonNull(getStr(httpContext, "bankCode"), UNKNOWN_BANK))
                .targetCode(getStr(httpContext, "targetCode"))
                .httpMethod(getStr(httpContext, "method"))
                .httpUri(getStr(httpContext, "uri"))
                .httpStatus(getInt(httpContext, "status"))
                .elapsedMs(getInt(httpContext, "elapsedMs"))
                .clientIp(getStr(httpContext, "clientIp"))
                .bodyData(bodyData)
                .errorCode(getStr(httpContext, "errorCode"))
                .errorMessage(getStr(httpContext, "errorMessage"))
                .build();
    }

    private String resolveControllerLogType(String message) {
        if (message.startsWith(MSG_CONTROLLER_REQ)) return "CONTROLLER_REQ";
        if (message.startsWith(MSG_CONTROLLER_RES)) return "CONTROLLER_RES";
        if (message.startsWith(MSG_CONTROLLER_ERR)) return "CONTROLLER_ERR";
        return null;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // BankExternalApiAspect 로그 빌더
    // ═══════════════════════════════════════════════════════════════════════

    // BankCoreException → bankErrorCode/bankErrorMessage, BusinessException → errorCode/errorMessage 순으로 적용.
    private SystemLog buildBankLog(ILoggingEvent event) {
        String logType = resolveBankLogType(event.getMessage());
        if (logType == null) return null;

        Map<String, String> mdc         = event.getMDCPropertyMap();
        Map<String, Object> bankContext = extractNestedContext(event, "http");

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
                .httpMethod(getStr(bankContext, "httpMethod"))
                .httpUri(getStr(bankContext, "httpUri"))
                .httpStatus(getInt(bankContext, "httpStatus"))
                .elapsedMs(getInt(bankContext, "elapsedMs"))
                .bodyData(resolveBankBodyData(logType, bankContext))
                .errorCode(errorCode)
                .errorMessage(errorMessage)
                .build();
    }

    private String resolveBankBodyData(String logType, Map<String, Object> bankContext) {
        return switch (logType) {
            case "BANK_REQ" -> getStr(bankContext, "request");
            case "BANK_RES" -> getStr(bankContext, "response");
            default         -> null;
        };
    }

    private String resolveBankLogType(String message) {
        if (message.startsWith(MSG_BANK_REQ))     return "BANK_REQ";
        if (message.startsWith(MSG_BANK_RES))     return "BANK_RES";
        if (message.startsWith(MSG_BANK_BIZ_ERR)) return "BANK_ERR";
        // [BankAPI][SystemError] → null 반환으로 DB 저장 스킵, 파일 로그에만 기록
        return null;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 구조화 인자 추출 — MapEntriesAppendingMarker 리플렉션
    // ═══════════════════════════════════════════════════════════════════════

    // entries(Map.of("http", httpContext)) 구조에서 contextKey에 해당하는 중첩 맵 추출.
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

    // MapEntriesAppendingMarker 내부 맵(private)을 리플렉션으로 꺼내 contextKey 값 반환. 실패 시 null.
    // logstash-logback-encoder 7.4는 getMap() 미제공 → 리플렉션 불가피
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

    // Spring Bean 조회. 미준비 시 null 반환.
    private SystemLogMapper resolveMapper() {
        if (mapper == null) {
            mapper = SpringContextHolder.getBean(SystemLogMapper.class);
        }
        return mapper;
    }
}

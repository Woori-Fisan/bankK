package com.woorifisan.platform.global.logging.appender;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import com.fasterxml.jackson.databind.ObjectMapper;
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

    // ── body JSON 파싱용 — Appender는 Spring Bean이 아니므로 자체 인스턴스 사용 ──
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    // 리플렉션 비용 절감을 위해 클래스 로드 시 1회 추출. 실패 시 null 유지.
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

    // REQ/RES: body_data = args[0], ERR: body_data = null + error_code/message 파싱.
    // 알 수 없는 접두어면 null 반환.
    private SystemLog buildControllerLog(ILoggingEvent event) {
        String logType = resolveControllerLogType(event.getMessage());
        if (logType == null) return null;

        Map<String, String> mdc         = event.getMDCPropertyMap();
        Map<String, Object> httpContext = extractNestedContext(event, "http");
        Object[]            args        = event.getArgumentArray();

        // args[0] = argsJson(REQ) / resultJson(RES) / ApiResponse.error JSON(ERR)
        String rawBody = (args != null && args.length > 0) ? String.valueOf(args[0]) : null;
        boolean isError = "CONTROLLER_ERR".equals(logType);
        boolean isReq   = "CONTROLLER_REQ".equals(logType);

        // REQ: Aspect가 파라미터를 [elem1, elem2, ...] 배열로 직렬화 → 단일 원소면 벗겨냄
        String processedBody = isReq ? unwrapSingleElementArray(rawBody) : rawBody;
        String bodyData      = isError ? null : processedBody;
        String errorCode     = isError ? parseApiErrorField(rawBody, "code")    : null;
        String errorMessage  = isError ? parseApiErrorField(rawBody, "message") : null;

        // bankCode: REQ 시점에 ControllerLoggingAspect가 httpContext에 저장 → RES/ERR도 동일 맵 재사용
        String bankCode = firstNonNull(getStr(httpContext, "bankCode"), UNKNOWN_BANK);

        return SystemLog.builder()
                .createdAt(toLocalDateTime(event.getTimeStamp()))
                .level(event.getLevel().toString())
                .logType(logType)
                .traceId(mdc.get("traceId"))
                .staffId(mdc.get("staffId"))
                .bankCode(bankCode)
                .httpMethod(getStr(httpContext, "method"))
                .httpUri(getStr(httpContext, "uri"))
                .httpStatus(getInt(httpContext, "status"))
                .elapsedMs(getInt(httpContext, "elapsedMs"))
                .clientIp(getStr(httpContext, "clientIp"))
                .bodyData(bodyData)
                .errorCode(errorCode)
                .errorMessage(errorMessage)
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

    // BankCoreException → bankErrorCode/bankErrorMessage, BusinessException → errorCode/errorMessage 순으로 적용.
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

    // {"success":false,"error":{"code":"...","message":"..."}} 구조에서 fieldName 값 추출
    private String parseApiErrorField(String bodyJson, String fieldName) {
        if (bodyJson == null) return null;
        try {
            return OBJECT_MAPPER.readTree(bodyJson).path("error").path(fieldName).asText(null);
        } catch (Exception e) {
            return null;
        }
    }

    // [elem1, elem2, ...] 배열에서 JSON 오브젝트만 추출. 1개면 그대로, 여러 개면 배열, 0개면 원본 반환.
    // ex) [1, {"bankCode":"020","amount":10}] → {"bankCode":"020","amount":10}
    private String unwrapSingleElementArray(String json) {
        if (json == null) return null;
        try {
            var node = OBJECT_MAPPER.readTree(json);
            if (!node.isArray()) return json;

            var objectNodes = new java.util.ArrayList<com.fasterxml.jackson.databind.JsonNode>();
            for (var elem : node) {
                if (elem.isObject()) objectNodes.add(elem);
            }

            if (objectNodes.size() == 1) {
                return OBJECT_MAPPER.writeValueAsString(objectNodes.get(0));
            }
            if (objectNodes.size() > 1) {
                return OBJECT_MAPPER.writeValueAsString(objectNodes);
            }
        } catch (Exception ignored) {}
        return json;
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

    // MapEntriesAppendingMarker에서 리플렉션으로 내부 맵을 꺼내 contextKey 값 반환. 실패 시 null.
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

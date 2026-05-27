package com.woorifisan.platform.global.security.aspect;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.woorifisan.platform.global.exception.BusinessException;
import com.woorifisan.platform.global.response.ErrorCode;
import com.woorifisan.platform.global.util.CryptoUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.Map;

/**
 * @VerifyTerminalSignature 애노테이션이 붙은 메소드 실행 전 JWS 서명을 검증하는 Aspect입니다.
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class TerminalSignatureAspect {

    @Value("${TERMINAL_SECURITY_PUBLIC_KEY}")
    private String terminalPublicKey;

    private final ObjectMapper objectMapper;

    /**
     * @VerifyTerminalSignature가 붙은 메소드 실행 전 서명 및 무결성 검증을 수행합니다.
     */
    @Before("@annotation(com.woorifisan.platform.global.security.annotation.VerifyTerminalSignature)")
    public void verifySignature(JoinPoint joinPoint) throws Throwable {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
        
        // 1. 헤더에서 JWS 서명 추출
        String jwsSignature = request.getHeader("x-jws-signature");
        if (jwsSignature == null || jwsSignature.isBlank()) {
            log.warn("JWS 서명 누락 - 요청 차단");
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }

        // 2. JWS 서명 검증 및 페이로드 추출
        String formattedKey = terminalPublicKey.replace("\\n", "\n");
        String payloadJson = CryptoUtil.verifyJwsAndGetPayload(jwsSignature, formattedKey);
        
        if (payloadJson == null) {
            log.warn("JWS 서명 검증 실패 - 요청 차단");
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }

        // 3. 무결성 및 Replay Attack 검증
        try {
            JsonNode payloadNode = objectMapper.readTree(payloadJson);
            
            // 3-1. Replay Attack 방지 (Timestamp 검증)
            long timestamp = payloadNode.path("timestamp").asLong();
            long currentTime = System.currentTimeMillis();
            if (currentTime - timestamp > 5 * 60 * 1000 || timestamp > currentTime + 60000) {
                log.warn("JWS Timestamp 만료 또는 미래 시간 - Replay Attack 의심");
                throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
            }

            // 3-2. 데이터 무결성 검증 (서명된 필드와 실제 Request Body 비교)
            Object body = findRequestBody(joinPoint);
            if (body != null) {
                JsonNode bodyNode = objectMapper.valueToTree(body);
                verifyIntegrity(payloadNode, bodyNode);
            }

        } catch (BusinessException be) {
            throw be;
        } catch (Exception e) {
            log.error("JWS 검증 과정 중 오류 발생", e);
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }
    }

    /**
     * 메소드 인자 중 @RequestBody 애노테이션이 붙은 객체를 찾습니다.
     */
    private Object findRequestBody(JoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Annotation[][] parameterAnnotations = method.getParameterAnnotations();
        Object[] args = joinPoint.getArgs();

        for (int i = 0; i < parameterAnnotations.length; i++) {
            for (Annotation annotation : parameterAnnotations[i]) {
                if (annotation instanceof RequestBody) {
                    return args[i];
                }
            }
        }
        
        // 차선책: String이나 Number가 아닌 첫 번째 객체 반환
        if (args != null) {
            for (Object arg : args) {
                if (arg != null && !(arg instanceof String) && !(arg instanceof Number)) {
                    return arg;
                }
            }
        }
        return null;
    }

    /**
     * 서명된 페이로드의 값이 실제 요청 바디의 값과 일치하는지 확인합니다.
     */
    private void verifyIntegrity(JsonNode payloadNode, JsonNode bodyNode) {
        Iterator<Map.Entry<String, JsonNode>> fields = payloadNode.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = fields.next();
            String key = entry.getKey();
            
            if ("timestamp".equals(key)) continue;

            JsonNode signedValue = entry.getValue();
            JsonNode actualValue = bodyNode.path(key);

            if (actualValue.isMissingNode() || !signedValue.equals(actualValue)) {
                log.warn("데이터 무결성 검증 실패 - 필드: {}, 서명된 값과 실제 값이 다릅니다.", key);
                throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
            }
        }
    }
}

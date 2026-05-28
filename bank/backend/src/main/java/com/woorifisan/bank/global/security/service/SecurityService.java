package com.woorifisan.bank.global.security.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.woorifisan.bank.domain.key.service.BankRsaKeyService;
import com.woorifisan.bank.global.exception.BusinessException;
import com.woorifisan.bank.global.response.ErrorCode;
import com.woorifisan.bank.global.security.dto.SecureRequest;
import com.woorifisan.bank.global.util.CryptoUtil;
import javax.crypto.SecretKey;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 은행 코어 보안 서비스
 * 암호화된 요청(SecureRequest)을 복호화하여 실제 DTO 객체로 변환하고 응답을 암호화합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SecurityService {

    private final CryptoUtil cryptoUtil;
    private final BankRsaKeyService bankRsaKeyService;
    private final ObjectMapper objectMapper;

    /**
     * 보안 요청 객체를 복호화하여 타겟 클래스 객체로 변환합니다.
     * 
     * @param request SecureRequest를 상속받은 보안 요청 객체
     * @param targetClass 복호화된 데이터를 매핑할 클래스
     * @return 복호화 및 매핑된 객체
     */
    public <T> T decrypt(SecureRequest request, Class<T> targetClass) {
        return decryptWithKey(request, targetClass).getData();
    }

    /**
     * 보안 요청 객체를 복호화하고 사용된 대칭키(CEK)를 함께 반환합니다.
     * 
     * @param request SecureRequest를 상속받은 보안 요청 객체
     * @param targetClass 복호화된 데이터를 매핑할 클래스
     * @return 복호화 데이터와 CEK가 포함된 결과 객체
     */
    public <T> DecryptionResult<T> decryptWithKey(SecureRequest request, Class<T> targetClass) {
        try {
            // 1. Key ID로 개인키 조회
            String privateKeyPem = bankRsaKeyService.getRawPrivateKeyByKeyId(request.getBankKeyId());

            // 2. JWE 복호화 및 CEK 추출
            CryptoUtil.JweDecryptionResult result = cryptoUtil.decryptJweWithKey(request.getReqPayload(), privateKeyPem);

            // 3. JSON 매핑
            T data = objectMapper.readValue(result.getPayload(), targetClass);

            return new DecryptionResult<>(data, result.getCek());
        } catch (BusinessException be) {
            throw be;
        } catch (Exception e) {
            log.error("보안 요청 복호화 및 CEK 추출 실패", e);
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
    }

    /**
     * 응답 데이터를 요청 시 사용된 대칭키(CEK)로 암호화합니다.
     * 
     * @param responseData 응답할 객체
     * @param cek 요청 시 사용되었던 대칭키
     * @return 암호화된 문자열 (resPayload)
     */
    public String encryptResponse(Object responseData, SecretKey cek) {
        try {
            String json = objectMapper.writeValueAsString(responseData);
            return cryptoUtil.encryptWithCek(json, cek);
        } catch (Exception e) {
            log.error("응답 데이터 암호화 실패", e);
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
    }

    /**
     * 복호화 결과와 CEK를 함께 담는 내부 클래스
     */
    @lombok.Getter
    @AllArgsConstructor
    public static class DecryptionResult<T> {
        private final T data;
        private final SecretKey cek;
    }
}


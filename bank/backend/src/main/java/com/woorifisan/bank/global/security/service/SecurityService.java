package com.woorifisan.bank.global.security.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.woorifisan.bank.domain.key.service.BankRsaKeyService;
import com.woorifisan.bank.global.exception.BusinessException;
import com.woorifisan.bank.global.response.ErrorCode;
import com.woorifisan.bank.global.security.dto.SecureRequest;
import com.woorifisan.bank.global.util.CryptoUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 은행 코어 보안 서비스
 * 암호화된 요청(SecureRequest)을 복호화하여 실제 DTO 객체로 변환합니다.
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
        try {
            // 1. Key ID로 개인키 조회
            String privateKeyPem = bankRsaKeyService.getRawPrivateKeyByKeyId(request.getBankKeyId());

            // 2. JWE 복호화
            String decryptedJson = cryptoUtil.decryptJwe(request.getReqPayload(), privateKeyPem);

            // 3. JSON 매핑
            return objectMapper.readValue(decryptedJson, targetClass);
        } catch (BusinessException be) {
            throw be;
        } catch (Exception e) {
            log.error("보안 요청 복호화 및 매핑 실패", e);
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
    }
}

package com.woorifisan.bank.domain.key.service;

import com.woorifisan.bank.domain.key.dto.request.BankRsaKeyRegisterRequest;
import com.woorifisan.bank.domain.key.dto.response.BankRsaKeyResponse;
import com.woorifisan.bank.domain.key.mapper.BankRsaKeyMapper;
import com.woorifisan.bank.domain.key.model.BankRsaKey;
import com.woorifisan.bank.global.exception.BusinessException;
import com.woorifisan.bank.global.response.ErrorCode;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * RSA 키 관리 서비스
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BankRsaKeyService {

    private final BankRsaKeyMapper bankRsaKeyMapper;

    /**
     * 가장 최근의 활성화된 RSA 공개키를 조회합니다.
     *
     * @return RSA 공개키 응답 DTO
     * @throws BusinessException 유효한 키가 없는 경우 RSA_KEY_NOT_FOUND 예외 발생
     */
    public BankRsaKeyResponse getLatestPublicKey() {
        BankRsaKey bankRsaKey = bankRsaKeyMapper.findLatestActiveKey()
                .orElseThrow(() -> new BusinessException(ErrorCode.RSA_KEY_NOT_FOUND));

        return BankRsaKeyResponse.of(bankRsaKey.getKeyId(), bankRsaKey.getPublicKey());
    }

    /**
     * 가장 최근의 활성화된 RSA 개인키를 평문으로 조회합니다. (서버 내부용)
     */
    public String getRawLatestPrivateKey() {
        BankRsaKey bankRsaKey = bankRsaKeyMapper.findLatestActiveKey()
                .orElseThrow(() -> new BusinessException(ErrorCode.RSA_KEY_NOT_FOUND));

        return bankRsaKey.getPrivateKeyEnc();
    }

    /**
     * 특정 Key ID를 가진 RSA 개인키를 평문으로 조회합니다. (서버 내부용)
     */
    public String getRawPrivateKeyByKeyId(String keyId) {
        BankRsaKey bankRsaKey = bankRsaKeyMapper.findByKeyId(keyId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RSA_KEY_NOT_FOUND));

        return bankRsaKey.getPrivateKeyEnc();
    }

    /**
     * 새로운 RSA 키를 등록합니다.
     *
     * @param request 공개키와 비밀키 정보
     * @return 등록된 키 정보 응답 DTO
     */
    @Transactional
    public BankRsaKeyResponse registerKey(BankRsaKeyRegisterRequest request) {
        String keyId = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();

        BankRsaKey bankRsaKey = BankRsaKey.of(
                keyId,
                request.getPublicKey(),
                request.getPrivateKey(),
                now,
                now.plusYears(10) // MySQL TIMESTAMP 범위를 고려하여 10년으로 수정 (최대 2038년)
        );

        bankRsaKeyMapper.insert(bankRsaKey);

        return BankRsaKeyResponse.of(bankRsaKey.getKeyId(), bankRsaKey.getPublicKey());
    }
}

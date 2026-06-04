package com.woorifisan.platform.crypto.service;

import com.woorifisan.platform.crypto.dto.response.BankRsaKeyResponse;
import com.woorifisan.platform.domain.bank.external.client.BankExternalClient;
import com.woorifisan.platform.global.config.BankNetworkConfig;
import com.woorifisan.platform.crypto.mapper.BankRsaKeyMapper;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 은행 RSA 공개키 관리를 담당하는 서비스입니다.
 * (DB의 bank 테이블을 직접 사용합니다.)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BankKeyService {

    private final BankExternalClient bankExternalClient;
    private final BankNetworkConfig bankNetworkConfig;
    private final BankRsaKeyMapper bankRsaKeyMapper;

    /**
     * 등록된 모든 은행의 최신 공개키 리스트를 조회합니다.
     */
    public Map<String, BankRsaKeyResponse> getAllLatestPublicKeys() {
        return bankNetworkConfig.getUrls().keySet().stream()
                .collect(Collectors.toMap(
                        bankCode -> bankCode,
                        this::getLatestPublicKey
                ));
    }

    /**
     * 특정 은행의 최신 공개키를 조회합니다.
     * DB에 없는 경우 은행 API를 호출하여 DB를 갱신한 후 반환합니다.
     */
    public BankRsaKeyResponse getLatestPublicKey(String bankCode) {
        BankRsaKeyResponse keyInfo = bankRsaKeyMapper.findKeyInfoByBankCode(bankCode);

        if (keyInfo != null && keyInfo.getPublicKey() != null) {
            return keyInfo;
        }

        // DB에 키가 없는 경우 즉시 갱신
        return refreshBankKey(bankCode);
    }

    /**
     * 은행 API를 호출하여 공개키를 가져오고, keyId 기반으로 변경사항이 있는 경우에만 DB를 업데이트합니다.
     */
    @Transactional
    public BankRsaKeyResponse refreshBankKey(String bankCode) {
        log.info("은행[{}] 공개키 DB 갱신 시도", bankCode);
        
        // 1. 은행 코어로부터 최신 키 가져오기
        BankRsaKeyResponse latestKey = bankExternalClient.fetchPublicKey(bankCode);

        // 2. DB에 저장된 기존 키 확인
        BankRsaKeyResponse existingKey = bankRsaKeyMapper.findKeyInfoByBankCode(bankCode);
        
        // 3. Key ID가 동일하다면 업데이트 건너뛰기
        if (existingKey != null && latestKey.getKeyId().equals(existingKey.getKeyId())) {
            log.info("은행[{}] 공개키 KeyID가 기존과 동일합니다. (KeyID: {}) DB 업데이트를 건너뜁니다.", bankCode, latestKey.getKeyId());
            return latestKey;
        }

        // 4. 키가 없거나 변경된 경우 DB 업데이트
        bankRsaKeyMapper.updateBankRsaKey(bankCode, latestKey.getKeyId(), latestKey.getPublicKey());
        log.info("은행[{}] 공개키가 DB에 갱신되었습니다. (New KeyID: {})", bankCode, latestKey.getKeyId());

        return latestKey;
    }
}

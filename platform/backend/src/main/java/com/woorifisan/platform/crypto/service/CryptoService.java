package com.woorifisan.platform.crypto.service;

import com.woorifisan.platform.global.exception.BusinessException;
import com.woorifisan.platform.global.response.ErrorCode;
import com.woorifisan.platform.crypto.dto.PublicKeyResponse;
import com.woorifisan.platform.crypto.dto.BankApiResponse;
import com.woorifisan.platform.crypto.infra.BankCryptoClient;
import org.springframework.stereotype.Service;
import com.woorifisan.platform.crypto.mapper.BankRsaKeyMapper;

@Service
public class CryptoService {

    private final BankRsaKeyMapper bankRsaKeyMapper;
    private final BankCryptoClient bankCryptoClient;

    public CryptoService(BankRsaKeyMapper bankRsaKeyMapper, BankCryptoClient bankCryptoClient) {
        this.bankRsaKeyMapper = bankRsaKeyMapper;
        this.bankCryptoClient = bankCryptoClient;
    }

    public PublicKeyResponse getPublicKey(String bankCode) {

        // 1. 플랫폼 DB (bank 테이블)에 해당 은행 코드가 존재하는지 조회
        if (!bankRsaKeyMapper.existsByBankCode(bankCode)) {
            throw new BusinessException(ErrorCode.BANK_NOT_FOUND);
        }

        // 2. 은행 백엔드(코어)에 공개키 요청 (WebClient 호출)
        BankApiResponse bankApiResponse = bankCryptoClient.fetchPublicKeyFromBank(bankCode);

        // 응답값 유효성 검증
        if (bankApiResponse == null || bankApiResponse.getKeyId() == null || bankApiResponse.getPublicKey() == null) {
            throw new BusinessException(ErrorCode.BANK_API_ERROR);
        }

        // 3. 응답받은 키 정보를 플랫폼 DB에 업데이트
        bankRsaKeyMapper.updateBankRsaKey(bankCode, bankApiResponse.getKeyId(), bankApiResponse.getPublicKey());

        // 4. 프론트엔드로 응답 반환
        return new PublicKeyResponse(bankCode, bankApiResponse.getPublicKey());
    }
}

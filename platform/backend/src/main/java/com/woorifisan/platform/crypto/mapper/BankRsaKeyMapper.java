package com.woorifisan.platform.crypto.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface BankRsaKeyMapper {
    boolean existsByBankCode(@Param("bankCode") String bankCode);

    void updateBankRsaKey(
            @Param("bankCode") String bankCode,
            @Param("keyId") String keyId,
            @Param("publicKey") String publicKey
    );

    com.woorifisan.platform.crypto.dto.response.BankRsaKeyResponse findKeyInfoByBankCode(@Param("bankCode") String bankCode);
}
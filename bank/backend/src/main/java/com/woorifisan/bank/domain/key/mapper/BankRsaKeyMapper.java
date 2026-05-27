package com.woorifisan.bank.domain.key.mapper;

import com.woorifisan.bank.domain.key.model.BankRsaKey;
import java.util.Optional;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface BankRsaKeyMapper {
    Optional<BankRsaKey> findById(Long id);

    /**
     * 가장 최근의 활성화된 RSA 키를 조회합니다.
     */
    Optional<BankRsaKey> findLatestActiveKey();

    /**
     * 특정 Key ID를 가진 RSA 키를 조회합니다.
     */
    Optional<BankRsaKey> findByKeyId(String keyId);

    /**
     * RSA 키를 저장합니다.
     */
    void insert(BankRsaKey bankRsaKey);
}

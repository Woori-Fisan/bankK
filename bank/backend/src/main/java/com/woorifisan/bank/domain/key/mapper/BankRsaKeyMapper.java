package com.woorifisan.bank.domain.key.mapper;

import com.woorifisan.bank.domain.key.model.BankRsaKey;
import java.util.Optional;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface BankRsaKeyMapper {
    Optional<BankRsaKey> findById(Long id);
}

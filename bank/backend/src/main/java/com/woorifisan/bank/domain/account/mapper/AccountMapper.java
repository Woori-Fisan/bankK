package com.woorifisan.bank.domain.account.mapper;

import com.woorifisan.bank.domain.account.model.Account;
import java.util.Optional;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AccountMapper {
    Optional<Account> findById(Long id);

    /**
     * 계좌번호 해시와 암호화된 주민번호 앞자리를 이용해 계좌 정보 조회
     */
    Optional<Account> findByAccountNoHashAndRrnPrefix(
            @Param("accountNoHash") String accountNoHash,
            @Param("rrnPrefixEnc") String rrnPrefixEnc
    );
}

package com.woorifisan.bank.domain.account.mapper;

import com.woorifisan.bank.domain.account.model.Account;
import java.math.BigDecimal;
import java.util.Optional;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AccountMapper {
    Optional<Account> findById(Long id);
    Optional<Account> findByIdForUpdate(Long id);

    Optional<Account> findByAccountNoHash(@Param("accountNoHash") String accountNoHash);

    Optional<Account> findByAccountNoHashAndRrnPrefix(
            @Param("accountNoHash") String accountNoHash,
            @Param("rrnPrefix") String rrnPrefix
    );

    int updateBalance(
            @Param("id") Long id,
            @Param("amount") BigDecimal amount
    );
}

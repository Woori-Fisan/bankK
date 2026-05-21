package com.woorifisan.bank.domain.account.mapper;

import com.woorifisan.bank.domain.account.model.Account;
import java.util.Optional;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AccountMapper {
    Optional<Account> findById(Long id);
}

package com.woorifisan.bank.domain.account.mapper;

import com.woorifisan.bank.domain.account.model.Account;
import java.util.Optional;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AccountMapper {
    Optional<Account> findById(Long id);
    
    /**
     * 계좌번호 해시로 계좌 정보 조회 (Blind Index 활용)
     */
    Optional<Account> findByAccountNoHash(@Param("accountNoHash") String accountNoHash);
}

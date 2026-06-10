package com.woorifisan.bank.domain.account.mapper;

import com.woorifisan.bank.domain.account.model.Account;
import java.math.BigDecimal;
import java.util.Optional;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AccountMapper {
    Optional<Account> findById(Long id);

    /**
     * 계좌번호 해시로 계좌 정보 조회 (Blind Index 활용)
     */
    Optional<Account> findByAccountNo(@Param("accountNo") String accountNo);
    Optional<Account> findByIdForUpdate(Long id);
    Optional<Account> findByAccountNoPlain(String accountNo);
    /**
     * 업데이트 (증액/차감 공용)
     * @return 수정된 행 수
     */
    int updateBalance(
            @Param("id") Long id,
            @Param("amount") BigDecimal amount
    );

    /**
     * 계좌번호와 주민번호 앞자리를 이용해 계좌 정보 조회
     */
    Optional<Account> findByAccountNoAndRrnPrefix(
            @Param("accountNo") String accountNo,
            @Param("rrnPrefix") String rrnPrefix
    );
}

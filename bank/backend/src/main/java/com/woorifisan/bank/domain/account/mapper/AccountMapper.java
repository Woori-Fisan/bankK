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
    Optional<Account> findByAccountNoPlain(String accountNo);
    void updateBalance(@Param("id") Long id, @Param("amount") BigDecimal amount);

    /**
     * 계좌번호와 주민번호 앞자리를 이용해 계좌 정보 조회
     */
    Optional<Account> findByAccountNoAndRrnPrefix(
            @Param("accountNo") String accountNo,
            @Param("rrnPrefix") String rrnPrefix
    );

    /**
     * 낙관적 락을 적용한 잔액 차감
     * @return 수정된 행 수 (0이면 버전 불일치)
     */
    int subtractBalance(
            @Param("id") Long id,
            @Param("amount") BigDecimal amount,
            @Param("version") Integer version
    );
}

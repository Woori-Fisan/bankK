package com.woorifisan.bank.domain.account.mapper;

import com.woorifisan.bank.domain.account.model.Account;
import java.math.BigDecimal;
import java.util.Optional;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AccountMapper {
    /**
     * ID로 계좌 조회
     */
    Optional<Account> findById(Long id);

    /**
     * ID로 계좌 조회 (비관적 락 적용 - FOR UPDATE)
     * 동시성 제어가 필요한 잔액 변경 작업 시 사용합니다.
     */
    Optional<Account> findByIdForUpdate(Long id);

    /**
     * 계좌번호 해시값으로 계좌 조회 (Blind Index)
     * 암호화된 계좌번호 대신 인덱싱이 가능한 해시값을 사용하여 조회 성능을 확보합니다.
     */
    Optional<Account> findByAccountNoHash(@Param("accountNoHash") String accountNoHash);

    /**
     * 계좌번호 해시와 고객 주민번호 앞자리를 조합하여 계좌 조회
     */
    Optional<Account> findByAccountNoHashAndRrnPrefix(
            @Param("accountNoHash") String accountNoHash,
            @Param("rrnPrefix") String rrnPrefix
    );

    /**
     * 잔액 업데이트
     * @param amount 증감할 금액 (음수면 차감, 양수면 증가)
     */
    int updateBalance(
            @Param("id") Long id,
            @Param("amount") BigDecimal amount
    );
}

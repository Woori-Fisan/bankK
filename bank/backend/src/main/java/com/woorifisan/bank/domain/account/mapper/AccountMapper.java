package com.woorifisan.bank.domain.account.mapper;

import com.woorifisan.bank.domain.account.dto.response.RecipientResponse;
import com.woorifisan.bank.domain.account.model.Account;
import java.math.BigDecimal;
import java.util.Optional;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AccountMapper {
    Optional<Account> findById(Long id);

    /**
     * 계좌 해시(Blind Index)로 수취인 정보 조회
     * @param accountNoHash 계좌번호 SHA-256 해시
     * @return 수취인 정보 (예금주명, 상태 등)
     */
    Optional<RecipientResponse> findRecipientByHash(@Param("accountNoHash") String accountNoHash);

    /**
     * 계좌 해시로 계좌 정보를 조회하며 비관적 락을 겁니다.
     * @param accountNoHash 계좌번호 SHA-256 해시
     * @return 계좌 모델
     */
    Optional<Account> findByAccountNoHashWithLock(@Param("accountNoHash") String accountNoHash);

    /**
     * 계좌 잔액을 업데이트합니다.
     * @param id 계좌 고유 ID
     * @param amount 업데이트할 잔액 (최종 잔액)
     * @return 영향받은 행 수
     */
    int updateBalance(@Param("id") Long id, @Param("balance") BigDecimal balance);
}

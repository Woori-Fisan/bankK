package com.woorifisan.bank.domain.account.mapper;

import com.woorifisan.bank.domain.account.model.TransactionLedger;
import java.util.Optional;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TransactionLedgerMapper {
    Optional<TransactionLedger> findByTxId(String txId);

    /**
     * 신규 거래 원장을 저장합니다.
     * @param ledger 거래 원장 객체
     * @return 영향받은 행 수
     */
    int insertLedger(TransactionLedger ledger);
}

package com.woorifisan.bank.domain.account.mapper;

import com.woorifisan.bank.domain.account.model.TransactionLedger;
import java.util.Optional;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TransactionLedgerMapper {
    Optional<TransactionLedger> findByTxId(String txId);
    void insert(TransactionLedger transactionLedger);
}

package com.woorifisan.bank.domain.account.mapper;

import com.woorifisan.bank.domain.account.model.TransactionLedger;
import java.util.List;
import java.util.Optional;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface TransactionLedgerMapper {
    Optional<TransactionLedger> findByTxId(String txId);

    /**
     * 특정 계좌의 기간별 거래 내역 목록 조회 (페이징 적용)
     */
    List<TransactionLedger> findHistoryList(
            @Param("accountId") Long accountId,
            @Param("startDate") String startDate,
            @Param("endDate") String endDate,
            @Param("offset") int offset,
            @Param("limit") int limit
    );

    /**
     * 특정 계좌의 기간별 거래 내역 전체 건수 조회
     */
    int countHistory(
            @Param("accountId") Long accountId,
            @Param("startDate") String startDate,
            @Param("endDate") String endDate
    );
}

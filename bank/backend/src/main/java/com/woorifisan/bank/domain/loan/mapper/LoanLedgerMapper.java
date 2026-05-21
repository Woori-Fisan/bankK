package com.woorifisan.bank.domain.loan.mapper;

import com.woorifisan.bank.domain.loan.model.LoanLedger;
import java.util.Optional;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface LoanLedgerMapper {
    Optional<LoanLedger> findById(Long id);
}

package com.woorifisan.bank.domain.loan.mapper;

import com.woorifisan.bank.domain.loan.model.LoanLedger;
import java.util.List;
import java.util.Optional;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface LoanLedgerMapper {
    Optional<LoanLedger> findById(Long id);
    Optional<LoanLedger> findByLoanNo(String loanNo);
    List<LoanLedger> findActiveByCustomerId(Long customerId);
    void insert(LoanLedger loanLedger);
    void updateExecution(LoanLedger loanLedger);
}

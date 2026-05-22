package com.woorifisan.bank.domain.loan.mapper;

import com.woorifisan.bank.domain.loan.model.LoanLedger;
import java.util.List;
import java.util.Optional;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface LoanLedgerMapper {
    Optional<LoanLedger> findById(Long id);
    Optional<LoanLedger> findByLoanNo(String loanNo);
    List<LoanLedger> findActiveByCustomerId(Long customerId);
    void insert(LoanLedger loanLedger);
    void updateExecution(LoanLedger loanLedger);
    // BK-B24: 동일 상품 60일 내 실행 이력 존재 여부
    boolean existsRecentActiveByCustomerAndProduct(
            @Param("customerId") Long customerId,
            @Param("productId") Long productId
    );
}

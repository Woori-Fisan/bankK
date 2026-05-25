package com.woorifisan.bank.domain.loan.mapper;

import com.woorifisan.bank.domain.loan.model.LoanProduct;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface LoanProductMapper {
    Optional<LoanProduct> findByProductId(Long productId);
    List<LoanProduct> findAllActive();
    List<LoanProduct> findMatchingProducts(
            @Param("approvedLimit") BigDecimal approvedLimit,
            @Param("appliedRate") BigDecimal appliedRate
    );
}

package com.woorifisan.bank.domain.loan.mapper;

import com.woorifisan.bank.domain.loan.model.LoanProduct;
import java.util.Optional;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface LoanProductMapper {
    Optional<LoanProduct> findByProductId(Long productId);
}

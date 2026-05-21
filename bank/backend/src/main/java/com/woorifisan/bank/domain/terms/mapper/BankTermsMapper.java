package com.woorifisan.bank.domain.terms.mapper;

import com.woorifisan.bank.domain.terms.model.BankTerms;
import java.util.Optional;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface BankTermsMapper {
    Optional<BankTerms> findById(Long id);
}

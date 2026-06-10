package com.woorifisan.bank.domain.customer.mapper;

import com.woorifisan.bank.domain.customer.model.Customer;
import java.util.Optional;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CustomerMapper {
    Optional<Customer> findById(Long id);
    Optional<Customer> findByCi(String ci);
}

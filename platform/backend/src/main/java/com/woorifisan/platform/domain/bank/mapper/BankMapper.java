package com.woorifisan.platform.domain.bank.mapper;

import com.woorifisan.platform.domain.bank.model.Bank;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

/**
 * 제휴 은행 데이터 접근 인터페이스
 */
@Mapper
public interface BankMapper {

    /**
     * ID로 은행 조회
     */
    Optional<Bank> findById(@Param("id") Long id);

    /**
     * 은행 코드로 조회
     */
    Optional<Bank> findByBankCode(@Param("bankCode") String bankCode);

    /**
     * 전체 은행 목록 조회
     */
    List<Bank> findAll();

}

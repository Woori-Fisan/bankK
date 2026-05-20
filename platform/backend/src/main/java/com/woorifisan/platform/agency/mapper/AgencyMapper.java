package com.woorifisan.platform.agency.mapper;

import com.woorifisan.platform.agency.model.Agency;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
public interface AgencyMapper {

    // ID로 대행기관 조회
    Optional<Agency> findById(@Param("id") Long id);

    // 대행기관 코드로 조회
    Optional<Agency> findByAgencyCode(@Param("agencyCode") String agencyCode);

    // 전체 대행기관 목록 조회
    List<Agency> findAll();

}

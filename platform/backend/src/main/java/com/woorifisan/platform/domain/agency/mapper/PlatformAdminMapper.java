package com.woorifisan.platform.domain.agency.mapper;

import com.woorifisan.platform.domain.agency.model.PlatformAdmin;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Optional;

/**
 * 플랫폼 운영자 데이터 접근 인터페이스
 */
@Mapper
public interface PlatformAdminMapper {

    // ID로 운영자 조회
    Optional<PlatformAdmin> findById(@Param("id") Long id);

    // 로그인 ID로 운영자 조회
    Optional<PlatformAdmin> findByLoginId(@Param("loginId") String loginId);
}

package com.woorifisan.platform.user.mapper;

import com.woorifisan.platform.user.model.PlatformAdmin;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

/**
 * 플랫폼 운영자 데이터 접근 인터페이스
 */
@Mapper
public interface PlatformAdminMapper {

    // 로그인 ID로 운영자 조회
    Optional<PlatformAdmin> findByLoginId(@Param("loginId") String loginId);
}

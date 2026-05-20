package com.woorifisan.platform.domain.auth.mapper;

import com.woorifisan.platform.domain.auth.model.PlatformUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AuthMapper {

    // loginId로 사용자 조회
    PlatformUser findByLoginId(@Param("loginId") String loginId);

    // 로그인 실패 횟수 증가
    void increaseFailedLoginCount(@Param("id") Long id);

    // 계정 잠금 처리 (5회 실패 시)
    void updateIsLocked(@Param("id") Long id, @Param("isLocked") boolean isLocked);

    // 로그인 성공 시 실패 횟수 초기화
    void resetFailedLoginCount(@Param("id") Long id);
}
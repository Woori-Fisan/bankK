package com.woorifisan.platform.domain.auth.mapper;

import com.woorifisan.platform.domain.auth.model.PlatformUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AuthMapper {

    // loginId로 사용자 조회
    PlatformUser findByLoginId(@Param("loginId") String loginId);

    // 로그인 실패 횟수 증가 + 임계값 도달 시 원자적 잠금 처리
    void increaseFailedLoginCount(@Param("id") Long id, @Param("maxCount") int maxCount);

    // 계정 잠금/잠금 해제 (관리자 비밀번호 초기화 등 명시적 잠금 제어용)
    void updateIsLocked(@Param("id") Long id, @Param("isLocked") boolean isLocked);

    // 로그인 성공 시 실패 횟수 초기화
    void resetFailedLoginCount(@Param("id") Long id);
}
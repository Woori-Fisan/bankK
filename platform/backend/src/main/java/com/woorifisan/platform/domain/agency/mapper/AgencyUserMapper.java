package com.woorifisan.platform.domain.agency.mapper;

import com.woorifisan.platform.domain.agency.dto.EmployeeDto;
import com.woorifisan.platform.domain.agency.model.AgencyUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 대행업체 직원(Agency User) 데이터 접근 인터페이스
 */
@Mapper
public interface AgencyUserMapper {

    // 필터 조건(agencyId)에 맞는 직원 수 조회
    int countEmployees(@Param("agencyId") Long agencyId);

    // 필터 조건(agencyId)에 맞는 직원 목록 조회 (페이징)
    List<EmployeeDto> findEmployees(
            @Param("agencyId") Long agencyId,
            @Param("offset") int offset,
            @Param("size") int size
    );

    // 사번(loginId)으로 중복 여부 확인
    boolean existsByLoginId(@Param("loginId") String loginId);

    // 대행업체 내 사번(employeeNum) 중복 여부 확인 (삭제되지 않은 사용자 기준)
    boolean existsByEmployeeNum(@Param("agencyId") Long agencyId, @Param("employeeNum") String employeeNum);

    // 사번(loginId)으로 직원 상세 조회
    Optional<AgencyUser> findByLoginId(@Param("loginId") String loginId);

    // 신규 직원 등록
    int insertEmployee(AgencyUser agencyUser);

    // 직원 삭제 (논리 삭제: is_deleted = true, is_locked = true)
    int deleteEmployee(
            @Param("loginId") String loginId,
            @Param("updatedAt") LocalDateTime updatedAt
    );

    // 비밀번호 업데이트 (잠금 해제 포함)
    int updatePassword(
            @Param("loginId") String loginId,
            @Param("encodedPassword") String encodedPassword,
            @Param("updatedAt") LocalDateTime updatedAt
    );
}

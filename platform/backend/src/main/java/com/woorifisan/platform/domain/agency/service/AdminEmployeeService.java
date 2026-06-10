package com.woorifisan.platform.domain.agency.service;

import com.woorifisan.platform.domain.agency.dto.response.EmployeeDeleteResponse;
import com.woorifisan.platform.domain.agency.dto.EmployeeDto;
import com.woorifisan.platform.domain.agency.dto.response.EmployeePaginationResponse;
import com.woorifisan.platform.domain.agency.dto.request.EmployeePasswordResetRequest;
import com.woorifisan.platform.domain.agency.dto.response.EmployeePasswordResetResponse;
import com.woorifisan.platform.domain.agency.dto.request.EmployeeRegisterRequest;
import com.woorifisan.platform.domain.agency.mapper.AgencyMapper;
import com.woorifisan.platform.domain.agency.mapper.AgencyUserMapper;
import com.woorifisan.platform.domain.agency.model.AgencyUser;
import com.woorifisan.platform.global.exception.BusinessException;
import com.woorifisan.platform.global.response.ErrorCode;
import com.woorifisan.platform.global.util.CryptoUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 대행업체 직원(Agency User) 관리 비즈니스 로직
 */
@Service
@RequiredArgsConstructor
public class AdminEmployeeService {

    private final AgencyUserMapper agencyUserMapper;
    private final AgencyMapper agencyMapper;
    private final PasswordEncoder passwordEncoder;

    @Value("${PLATFORM_SECURITY_PRIVATE_KEY}")
    private String platformPrivateKey;

    /**
     * 4.3.1. 직원 목록 조회
     */
    @Transactional(readOnly = true)
    public EmployeePaginationResponse getEmployeeList(int page, int size, Long agencyId) {
        int offset = page * size;
        
        // 1. 조건에 맞는 전체 카운트 조회
        int totalCount = agencyUserMapper.countEmployees(agencyId);

        // 2. 전체 페이지 수 계산
        int totalPages = (totalCount == 0) ? 0 : (int) Math.ceil((double) totalCount / size);
        
        // 3. 조건에 맞는 직원 목록 페이징 조회
        List<EmployeeDto> employees = agencyUserMapper.findEmployees(agencyId, offset, size);

        return new EmployeePaginationResponse(totalCount, totalPages, page, employees);
    }

    /**
     * 4.3.2. 직원 등록
     */
    @Transactional
    public void registerEmployee(EmployeeRegisterRequest request) {
        // 1. 유저 아이디(로그인 ID) 중복 확인 [USER_002]
        boolean isUserExist = agencyUserMapper.existsByLoginId(request.getLoginId());
        if (isUserExist) {
            throw new BusinessException(ErrorCode.USER_ALREADY_EXISTS);
        }

        // 2. 대행업체 존재 여부 확인 [EMPLOYEE_002]
        boolean isAgencyExist = agencyMapper.existsById(request.getAgencyId());
        if (!isAgencyExist) {
            throw new BusinessException(ErrorCode.NON_EXISTENT_AGENCY);
        }

        // 3. 대행업체 내 사번 중복 확인 (삭제되지 않은 사용자 기준) [EMPLOYEE_003]
        boolean isEmployeeNumExist = agencyUserMapper.existsByEmployeeNum(request.getAgencyId(), request.getEmployeeNum());
        if (isEmployeeNumExist) {
            throw new BusinessException(ErrorCode.DUPLICATE_EMPLOYEE_NUM);
        }

        String decryptedPassword;
        try {
            String formattedPlatformPrivateKey = platformPrivateKey.replace("\\n", "\n");
            decryptedPassword = CryptoUtil.decryptJwe(request.getPassword(), formattedPlatformPrivateKey);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }

        // 3. 패스워드 인코딩
        String encodedPassword = passwordEncoder.encode(decryptedPassword);

        // 4. 모델(Entity) 생성 후 DB 저장 및 오류 처리 [ERR_002]
        AgencyUser employee = AgencyUser.of(request.getLoginId(), encodedPassword, request.getRole(), request.getAgencyId(), request.getEmployeeNum());
        int insertedRows = agencyUserMapper.insertEmployee(employee);
        if (insertedRows == 0) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 4.3.3. 직원 삭제
     */
    @Transactional
    public EmployeeDeleteResponse deleteEmployee(String loginId) {
        // 1. 기존 유저 존재 여부 확인 [USER_001]
        boolean isUserExist = agencyUserMapper.existsByLoginId(loginId);
        if (!isUserExist) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        LocalDateTime now = LocalDateTime.now();

        // 2. 논리 삭제 처리 후 오류 처리 [ERR_002]
        int updatedRows = agencyUserMapper.deleteEmployee(loginId, now);
        if (updatedRows == 0) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
        }

        return new EmployeeDeleteResponse(loginId, true, now);
    }

    /**
     * 4.3.4. 비밀번호 초기화
     */
    @Transactional
    public EmployeePasswordResetResponse resetPassword(String loginId, EmployeePasswordResetRequest request) {
        // 1. 기존 유저 존재 여부 확인 [USER_001]
        boolean isUserExist = agencyUserMapper.existsByLoginId(loginId);
        if (!isUserExist) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        // 2. 새 비밀번호 인코딩 처리
        String decryptedPassword;
        try {
            String formattedPlatformPrivateKey = platformPrivateKey.replace("\\n", "\n");
            decryptedPassword = CryptoUtil.decryptJwe(request.getPassword(), formattedPlatformPrivateKey);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }

        // 3. 패스워드 인코딩
        String encodedPassword = passwordEncoder.encode(decryptedPassword);

        LocalDateTime now = LocalDateTime.now();

        // 3. 비밀번호 업데이트 및 계정 잠금 해제 처리 오류 확인 [ERR_002]
        int updatedRows = agencyUserMapper.updatePassword(loginId, encodedPassword, now);
        if (updatedRows == 0) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
        }

        return new EmployeePasswordResetResponse(loginId, false, now);
    }
}
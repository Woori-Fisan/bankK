package com.woorifisan.platform.domain.agency.service;

import com.woorifisan.platform.domain.agency.dto.request.EmployeePasswordResetRequest;
import com.woorifisan.platform.domain.agency.dto.request.EmployeeRegisterRequest;
import com.woorifisan.platform.domain.agency.mapper.AgencyMapper;
import com.woorifisan.platform.domain.agency.mapper.AgencyUserMapper;
import com.woorifisan.platform.domain.agency.model.AgencyUser;
import com.woorifisan.platform.global.exception.BusinessException;
import com.woorifisan.platform.global.response.ErrorCode;
import com.woorifisan.platform.global.util.CryptoUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("직원 관리 서비스(AdminEmployeeService) 단위 테스트")
class AdminEmployeeServiceTest {

    @InjectMocks
    private AdminEmployeeService adminEmployeeService;

    @Mock
    private AgencyUserMapper agencyUserMapper;

    @Mock
    private AgencyMapper agencyMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    private final String dummyPrivateKey = "dummy-private-key";

    @Test
    @DisplayName("직원 등록 시 이미 존재하는 아이디면 예외가 발생한다")
    void 직원_등록_아이디_중복_예외_테스트() {
        // given
        EmployeeRegisterRequest request = new EmployeeRegisterRequest("testUser", "encPass", "AGENCY_USER", 1L, "EMP001");
        when(agencyUserMapper.existsByLoginId(anyString())).thenReturn(true);

        // when & then
        BusinessException exception = assertThrows(BusinessException.class, () -> adminEmployeeService.registerEmployee(request));
        assertEquals(ErrorCode.USER_ALREADY_EXISTS, exception.getErrorCode());
    }

    @Test
    @DisplayName("직원 등록 시 소속 대행업체가 존재하지 않으면 예외가 발생한다")
    void 직원_등록_대행업체_미존재_예외_테스트() {
        // given
        EmployeeRegisterRequest request = new EmployeeRegisterRequest("testUser", "encPass", "AGENCY_USER", 1L, "EMP001");
        when(agencyUserMapper.existsByLoginId(anyString())).thenReturn(false);
        when(agencyMapper.existsById(anyLong())).thenReturn(false);

        // when & then
        BusinessException exception = assertThrows(BusinessException.class, () -> adminEmployeeService.registerEmployee(request));
        assertEquals(ErrorCode.NON_EXISTENT_AGENCY, exception.getErrorCode());
    }

    @Test
    @DisplayName("직원 등록 시 해당 업체 내에 이미 존재하는 사번이면 예외가 발생한다")
    void 직원_등록_사번_중복_예외_테스트() {
        // given
        EmployeeRegisterRequest request = new EmployeeRegisterRequest("testUser", "encPass", "AGENCY_USER", 1L, "EMP001");
        when(agencyUserMapper.existsByLoginId(anyString())).thenReturn(false);
        when(agencyMapper.existsById(anyLong())).thenReturn(true);
        when(agencyUserMapper.existsByEmployeeNum(anyLong(), anyString())).thenReturn(true);

        // when & then
        BusinessException exception = assertThrows(BusinessException.class, () -> adminEmployeeService.registerEmployee(request));
        assertEquals(ErrorCode.USER_ALREADY_EXISTS, exception.getErrorCode());
    }

    @Test
    @DisplayName("직원 등록 시 비밀번호 복호화에 실패하면 예외가 발생한다")
    void 직원_등록_비밀번호_복호화_실패_예외_테스트() {
        // given
        ReflectionTestUtils.setField(adminEmployeeService, "platformPrivateKey", dummyPrivateKey);
        EmployeeRegisterRequest request = new EmployeeRegisterRequest("testUser", "invalidEncPass", "AGENCY_USER", 1L, "EMP001");
        
        when(agencyUserMapper.existsByLoginId(anyString())).thenReturn(false);
        when(agencyMapper.existsById(anyLong())).thenReturn(true);
        when(agencyUserMapper.existsByEmployeeNum(anyLong(), anyString())).thenReturn(false);

        try (MockedStatic<CryptoUtil> cryptoUtil = mockStatic(CryptoUtil.class)) {
            cryptoUtil.when(() -> CryptoUtil.decryptJwe(anyString(), anyString())).thenThrow(new RuntimeException("Decryption error"));

            // when & then
            BusinessException exception = assertThrows(BusinessException.class, () -> adminEmployeeService.registerEmployee(request));
            assertEquals(ErrorCode.INVALID_CREDENTIALS, exception.getErrorCode());
        }
    }

    @Test
    @DisplayName("직원 등록 시 DB 저장 결과가 0이면 내부 서버 에러 예외가 발생한다")
    void 직원_등록_DB_저장_실패_예외_테스트() {
        // given
        ReflectionTestUtils.setField(adminEmployeeService, "platformPrivateKey", dummyPrivateKey);
        EmployeeRegisterRequest request = new EmployeeRegisterRequest("testUser", "encPass", "AGENCY_USER", 1L, "EMP001");
        
        when(agencyUserMapper.existsByLoginId(anyString())).thenReturn(false);
        when(agencyMapper.existsById(anyLong())).thenReturn(true);
        when(agencyUserMapper.existsByEmployeeNum(anyLong(), anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashedPass");
        when(agencyUserMapper.insertEmployee(any(AgencyUser.class))).thenReturn(0);

        try (MockedStatic<CryptoUtil> cryptoUtil = mockStatic(CryptoUtil.class)) {
            cryptoUtil.when(() -> CryptoUtil.decryptJwe(anyString(), anyString())).thenReturn("decryptedPass");

            // when & then
            BusinessException exception = assertThrows(BusinessException.class, () -> adminEmployeeService.registerEmployee(request));
            assertEquals(ErrorCode.INTERNAL_SERVER_ERROR, exception.getErrorCode());
        }
    }

    @Test
    @DisplayName("직원 삭제 시 존재하지 않는 아이디면 예외가 발생한다")
    void 직원_삭제_사용자_미존재_예외_테스트() {
        // given
        when(agencyUserMapper.existsByLoginId(anyString())).thenReturn(false);

        // when & then
        BusinessException exception = assertThrows(BusinessException.class, () -> adminEmployeeService.deleteEmployee("unknownUser"));
        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("직원 삭제 - DB 업데이트 실패 시 내부 서버 에러 예외가 발생한다")
    void 직원_삭제_DB_처리_실패_예외_테스트() {
        // given
        when(agencyUserMapper.existsByLoginId(anyString())).thenReturn(true);
        when(agencyUserMapper.deleteEmployee(anyString(), any(LocalDateTime.class))).thenReturn(0);

        // when & then
        BusinessException exception = assertThrows(BusinessException.class, () -> adminEmployeeService.deleteEmployee("testUser"));
        assertEquals(ErrorCode.INTERNAL_SERVER_ERROR, exception.getErrorCode());
    }

    @Test
    @DisplayName("비밀번호 초기화 시 존재하지 않는 아이디면 예외가 발생한다")
    void 비밀번호_초기화_사용자_미존재_예외_테스트() {
        // given
        EmployeePasswordResetRequest request = new EmployeePasswordResetRequest("newEncPass");
        when(agencyUserMapper.existsByLoginId(anyString())).thenReturn(false);

        // when & then
        BusinessException exception = assertThrows(BusinessException.class, () -> adminEmployeeService.resetPassword("unknownUser", request));
        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("비밀번호 초기화 시 복호화에 실패하면 예외가 발생한다")
    void 비밀번호_초기화_복호화_실패_예외_테스트() {
        // given
        ReflectionTestUtils.setField(adminEmployeeService, "platformPrivateKey", dummyPrivateKey);
        EmployeePasswordResetRequest request = new EmployeePasswordResetRequest("invalidEncPass");
        
        when(agencyUserMapper.existsByLoginId(anyString())).thenReturn(true);

        try (MockedStatic<CryptoUtil> cryptoUtil = mockStatic(CryptoUtil.class)) {
            cryptoUtil.when(() -> CryptoUtil.decryptJwe(anyString(), anyString())).thenThrow(new RuntimeException("Decryption error"));

            // when & then
            BusinessException exception = assertThrows(BusinessException.class, () -> adminEmployeeService.resetPassword("testUser", request));
            assertEquals(ErrorCode.INVALID_CREDENTIALS, exception.getErrorCode());
        }
    }

    @Test
    @DisplayName("비밀번호 초기화 - DB 업데이트 실패 시 내부 서버 에러 예외가 발생한다")
    void 비밀번호_초기화_DB_처리_실패_예외_테스트() {
        // given
        ReflectionTestUtils.setField(adminEmployeeService, "platformPrivateKey", dummyPrivateKey);
        EmployeePasswordResetRequest request = new EmployeePasswordResetRequest("newEncPass");
        
        when(agencyUserMapper.existsByLoginId(anyString())).thenReturn(true);
        when(passwordEncoder.encode(anyString())).thenReturn("newHashedPass");
        when(agencyUserMapper.updatePassword(anyString(), anyString(), any(LocalDateTime.class))).thenReturn(0);

        try (MockedStatic<CryptoUtil> cryptoUtil = mockStatic(CryptoUtil.class)) {
            cryptoUtil.when(() -> CryptoUtil.decryptJwe(anyString(), anyString())).thenReturn("newDecryptedPass");

            // when & then
            BusinessException exception = assertThrows(BusinessException.class, () -> adminEmployeeService.resetPassword("testUser", request));
            assertEquals(ErrorCode.INTERNAL_SERVER_ERROR, exception.getErrorCode());
        }
    }
}

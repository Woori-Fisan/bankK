package com.woorifisan.monitoring.domain.user.service;

import com.woorifisan.monitoring.domain.user.dto.AuthTokenDto;
import com.woorifisan.monitoring.domain.user.dto.request.LoginRequest;
import com.woorifisan.monitoring.domain.user.dto.request.RegisterRequest;
import com.woorifisan.monitoring.domain.user.mapper.UserMapper;
import com.woorifisan.monitoring.domain.user.model.User;
import com.woorifisan.monitoring.global.config.JwtProvider;
import com.woorifisan.monitoring.global.exception.BusinessException;
import com.woorifisan.monitoring.global.response.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 모니터링 시스템 운영 관리자 비즈니스 서비스
 * 사용자 정보 관리와 인증 흐름의 오케스트레이션을 담당합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;

    /**
     * 로그인
     */
    public AuthTokenDto login(LoginRequest request) {
        log.info("[로그인 시도] loginId: {}", request.getLoginId());

        // 1. 사용자 조회 및 유효성 검증
        User user = validateUser(request.getLoginId());

        // 2. 비밀번호 검증
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            log.warn("[로그인 실패] 비밀번호 불일치 - loginId: {}", request.getLoginId());
            throw new BusinessException(ErrorCode.LOGIN_FAILED);
        }

        // 3. 토큰 발급 및 저장
        String accessToken = jwtProvider.generateAccessToken(user.getId(), user.getLoginId());
        String refreshToken = jwtProvider.generateRefreshToken(user.getId(), user.getLoginId());
        
        jwtProvider.saveRefreshToken(user.getId(), refreshToken);

        log.info("[로그인 성공] staffId: {}, loginId: {}", user.getId(), user.getLoginId());

        return buildAuthTokenDto(user.getLoginId(), accessToken, refreshToken);
    }

    /**
     * 토큰 재발급 (Refresh Token Rotation - RTR)
     */
    public AuthTokenDto refresh(String refreshToken) {
        // 1. Refresh Token 유효성 검증
        jwtProvider.validateToken(refreshToken);

        // 2. 사용자 정보 추출
        Long userId = jwtProvider.extractStaffId(refreshToken);
        String loginId = jwtProvider.extractLoginId(refreshToken);

        // 3. Redis에서 저장된 토큰 조회
        String storedRefreshToken = jwtProvider.getStoredRefreshToken(userId);

        // 4. Redis에 없으면 이미 로그아웃된 상태
        if (storedRefreshToken == null) {
            log.warn("[재발급 실패] 이미 로그아웃된 세션 - loginId: {}", loginId);
            throw new BusinessException(ErrorCode.ALREADY_LOGGED_OUT);
        }

        // 5. RTR — 토큰 재사용 감지
        if (!storedRefreshToken.equals(refreshToken)) {
            jwtProvider.deleteRefreshToken(userId);
            jwtProvider.addToBlacklist(refreshToken);
            log.error("[보안 경고] Refresh Token 재사용 감지 - staffId: {} 세션 강제 종료", userId);
            throw new BusinessException(ErrorCode.TOKEN_REUSE_DETECTED);
        }

        // 6. 사용자 유효성 재확인
        validateUser(loginId);

        // 7. 새 토큰 쌍 발급 및 갱신
        String newAccessToken = jwtProvider.generateAccessToken(userId, loginId);
        String newRefreshToken = jwtProvider.generateRefreshToken(userId, loginId);
        
        jwtProvider.saveRefreshToken(userId, newRefreshToken);

        log.info("[재발급 성공] loginId: {}", loginId);

        return buildAuthTokenDto(loginId, newAccessToken, newRefreshToken);
    }

    /**
     * 계정 등록
     */
    @Transactional
    public void register(RegisterRequest request) {
        log.info("[회원가입 시도] loginId: {}", request.getLoginId());

        if (userMapper.findByLoginId(request.getLoginId()) != null) {
            log.warn("[회원가입 실패] 중복된 아이디 - loginId: {}", request.getLoginId());
            throw new BusinessException(ErrorCode.DUPLICATE_LOGIN_ID);
        }

        String passwordHash = passwordEncoder.encode(request.getPassword());
        User user = User.of(request.getLoginId(), passwordHash, request.getName());
        userMapper.save(user);

        log.info("[회원가입 성공] loginId: {}, name: {}", user.getLoginId(), user.getName());
    }

    /**
     * 로그아웃
     */
    public void logout(Long userId, String accessToken) {
        log.info("[로그아웃] staffId: {} 세션 무효화", userId);
        jwtProvider.deleteRefreshToken(userId);
        if (accessToken != null) {
            jwtProvider.addToBlacklist(accessToken);
        }
    }

    /**
     * 사용자 정보 및 상태 검증
     */
    private User validateUser(String loginId) {
        User user = userMapper.findByLoginId(loginId);
        if (user == null) {
            log.warn("[검증 실패] 존재하지 않는 사용자 - loginId: {}", loginId);
            throw new BusinessException(ErrorCode.LOGIN_FAILED);
        }

        if (!user.isActive()) {
            log.warn("[검증 실패] 비활성화된 계정 - loginId: {}", loginId);
            throw new BusinessException(ErrorCode.LOGIN_FAILED);
        }
        return user;
    }

    /**
     * AuthTokenDto 조립
     */
    private AuthTokenDto buildAuthTokenDto(String loginId, String accessToken, String refreshToken) {
        return AuthTokenDto.builder()
                .accessToken(accessToken)
                .accessTokenExpiresIn(jwtProvider.getAccessTokenExpiration() / 1000)
                .refreshToken(refreshToken)
                .refreshTokenExpiresIn(jwtProvider.getRefreshTokenExpiration() / 1000)
                .loginId(loginId)
                .build();
    }
}

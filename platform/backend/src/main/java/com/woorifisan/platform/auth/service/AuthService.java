package com.woorifisan.platform.auth.service;

import com.woorifisan.platform.auth.dto.LoginRequest;
import com.woorifisan.platform.auth.dto.LoginResponse;
import com.woorifisan.platform.auth.dto.TokenRefreshRequest;
import com.woorifisan.platform.auth.dto.TokenRefreshResponse;
import com.woorifisan.platform.auth.mapper.AuthMapper;
import com.woorifisan.platform.auth.model.PlatformUser;
import com.woorifisan.platform.global.config.JwtProvider;
import com.woorifisan.platform.global.exception.BusinessException;
import com.woorifisan.platform.global.response.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final long REFRESH_TOKEN_EXPIRATION_SECONDS = 28800;
    private static final int MAX_FAILED_LOGIN_COUNT = 5;

    @Value("${REFRESH_TOKEN_EXPIRATION_SECONDS}")
    private long REFRESH_TOKEN_EXPIRATION_SECONDS;  // 이 선언이 없는 것

    private final AuthMapper authMapper;
    private final JwtProvider jwtProvider;
    private final PasswordEncoder passwordEncoder;
    private final RedisTemplate<String, String> redisTemplate;

    // 로그인
    public LoginResponse login(LoginRequest request) {

        // 1. 사용자 조회
        PlatformUser user = authMapper.findByLoginId(request.getEmployeeId());
        if (user == null) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }

        // 2. 계정 삭제 여부 확인
        if (user.isDeleted()) {
            throw new BusinessException(ErrorCode.ACCOUNT_DELETED);
        }

        // 3. 계정 잠금 여부 확인
        if (user.isLocked()) {
            throw new BusinessException(ErrorCode.ACCOUNT_LOCKED);
        }

        // 4. 비밀번호 검증
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            handleLoginFailure(user);
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }

        // 5. 로그인 성공 — 실패 횟수 초기화
        authMapper.resetFailedLoginCount(user.getId());

        // 6. 토큰 발급
        String accessToken = jwtProvider.generateAccessToken(user.getId(), user.getRole());
        String refreshToken = jwtProvider.generateRefreshToken(user.getId(), user.getRole());

        // 7. Redis에 staffId → refreshToken 저장 (8시간 TTL)
        redisTemplate.opsForValue().set(
                getRedisKey(user.getId()),
                refreshToken,
                REFRESH_TOKEN_EXPIRATION_SECONDS,
                TimeUnit.SECONDS
        );

        log.info("로그인 성공 - staffId: {}, role: {}", user.getId(), user.getRole());

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .role(user.getRole())
                .build();
    }

    // 로그아웃
    public void logout(Long staffId) {
        String key = getRedisKey(staffId);

        // Redis에서 즉시 삭제 (이미 없어도 안전하게 처리하여 멱등성 보장)
        redisTemplate.delete(key);
        log.info("로그아웃 성공 - staffId: {}", staffId);
    }

    // Access Token 재발급 (RTR)
    public TokenRefreshResponse refresh(TokenRefreshRequest request) {
        String refreshToken = request.getRefreshToken();

        // 1. Refresh Token 유효성 검증
        jwtProvider.validateToken(refreshToken);

        // 2. staffId 추출
        Long staffId = jwtProvider.extractStaffId(refreshToken);
        String role = jwtProvider.extractRole(refreshToken);

        // 3. Redis에서 저장된 Refresh Token 조회
        String storedRefreshToken = redisTemplate.opsForValue().get(getRedisKey(staffId));

        // 4. Redis에 없으면 이미 로그아웃된 상태
        if (storedRefreshToken == null) {
            throw new BusinessException(ErrorCode.ALREADY_LOGGED_OUT);
        }

        // 5. RTR — 토큰 재사용 감지 (Redis 저장값과 다르면 탈취 의심)
        if (!storedRefreshToken.equals(refreshToken)) {
            // 전체 세션 강제 로그아웃
            redisTemplate.delete(getRedisKey(staffId));
            log.warn("Refresh Token 재사용 감지 - staffId: {} 전체 세션 강제 로그아웃", staffId);
            throw new BusinessException(ErrorCode.TOKEN_REUSE_DETECTED);
        }

        // 6. 새 토큰 발급
        String newAccessToken = jwtProvider.generateAccessToken(staffId, role);
        String newRefreshToken = jwtProvider.generateRefreshToken(staffId, role);

        // 7. Redis 갱신
        redisTemplate.opsForValue().set(
                getRedisKey(staffId),
                newRefreshToken,
                REFRESH_TOKEN_EXPIRATION_SECONDS,
                TimeUnit.SECONDS
        );

        return TokenRefreshResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .accessTokenExpiresIn((int) (jwtProvider.getAccessTokenExpiration() / 1000))
                .refreshTokenExpiresIn((int) (jwtProvider.getRefreshTokenExpiration() / 1000))
                .build();
    }

    // 로그인 실패 처리
    private void handleLoginFailure(PlatformUser user) {
        int failedCount = user.getFailedLoginCount() + 1;
        authMapper.increaseFailedLoginCount(user.getId());

        if (failedCount >= MAX_FAILED_LOGIN_COUNT) {
            authMapper.updateIsLocked(user.getId(), true);
            log.warn("계정 잠금 처리 - staffId: {}, 실패 횟수: {}", user.getId(), failedCount);
            throw new BusinessException(ErrorCode.ACCOUNT_LOCKED);
        }

        log.warn("로그인 실패 - staffId: {}, 실패 횟수: {}/{}", user.getId(), failedCount, MAX_FAILED_LOGIN_COUNT);
    }

    // Redis Key 생성
    private String getRedisKey(Long staffId) {
        return "refresh:" + staffId;
    }
}
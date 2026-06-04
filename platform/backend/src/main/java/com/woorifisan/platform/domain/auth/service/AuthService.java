package com.woorifisan.platform.domain.auth.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.woorifisan.platform.domain.auth.dto.AuthTokenDto;
import com.woorifisan.platform.domain.auth.dto.request.LoginRequest;
import com.woorifisan.platform.domain.auth.dto.response.PublicAuthKeyResponse;
import com.woorifisan.platform.domain.auth.mapper.AuthMapper;
import com.woorifisan.platform.domain.auth.model.PlatformUser;
import com.woorifisan.platform.global.config.JwtProvider;
import com.woorifisan.platform.global.exception.BusinessException;
import com.woorifisan.platform.global.response.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.woorifisan.platform.global.util.CryptoUtil;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final int MAX_FAILED_LOGIN_COUNT = 5;

    @Value("${PLATFORM_SECURITY_PUBLIC_KEY}")
    private String platformPublicKey;

    @Value("${PLATFORM_SECURITY_PRIVATE_KEY}")
    private String platformPrivateKey;

    @Value("${TERMINAL_SECURITY_PUBLIC_KEY}")
    private String terminalPublicKey;

    private final AuthMapper authMapper;
    private final JwtProvider jwtProvider;
    private final PasswordEncoder passwordEncoder;
    private final RedisTemplate<String, String> redisTemplate;

    // 공개키 반환
    public PublicAuthKeyResponse getPublicKey() {
        // PEM 형식의 이스케이프된 줄바꿈 문자를 실제 줄바꿈으로 복원하여 반환
        String formattedKey = platformPublicKey.replace("\\n", "\n");
        return PublicAuthKeyResponse.builder()
                .publicKey(formattedKey)
                .build();
    }

    // 로그인
    public AuthTokenDto login(LoginRequest request, String jwsSignature) {

        // 1. 단말기 JWS 서명 검증 및 페이로드 추출
        String formattedTerminalPublicKey = terminalPublicKey.replace("\\n", "\n");
        String payloadJson = CryptoUtil.verifyJwsAndGetPayload(jwsSignature, formattedTerminalPublicKey);
        
        if (payloadJson == null) {
            log.warn("JWS 서명 검증 실패 - 로그인 요청 차단 (Employee: {})", request.getEmployeeId());
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }

        // 1-1. JWS 페이로드 무결성 및 Replay Attack 방지 (Timestamp 검증)
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode payloadNode = objectMapper.readTree(payloadJson);
            
            String payloadEmployeeId = payloadNode.path("employeeId").asText();
            String payloadPassword = payloadNode.path("password").asText(); // 이 값은 암호화된 비밀번호
            long timestamp = payloadNode.path("timestamp").asLong();

            // A. 데이터 위변조 확인 (요청 데이터와 서명된 데이터가 일치하는지)
            if (!request.getEmployeeId().equals(payloadEmployeeId) || !request.getPassword().equals(payloadPassword)) {
                log.warn("JWS 페이로드 데이터 불일치 (위변조 의심) - Employee: {}", request.getEmployeeId());
                throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
            }

            // B. Replay Attack 방지 (Timestamp가 현재 시간 기준 5분 이내인지 검증)
            long currentTime = System.currentTimeMillis();
            long allowedTimeWindow = 5 * 60 * 1000; // 5분 허용
            if (currentTime - timestamp > allowedTimeWindow || timestamp > currentTime + 60000) { // 미래 시간은 1분 허용
                log.warn("JWS Timestamp 만료 (Replay Attack 의심) - Employee: {}", request.getEmployeeId());
                throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
            }

        } catch (Exception e) {
            log.error("JWS 페이로드 검증 중 오류 발생", e);
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }

        // 2. JWE 비밀번호 복호화
        String decryptedPassword;
        try {
            String formattedPlatformPrivateKey = platformPrivateKey.replace("\\n", "\n");
            decryptedPassword = CryptoUtil.decryptJwe(request.getPassword(), formattedPlatformPrivateKey);
        } catch (Exception e) {
            log.error("JWE 비밀번호 복호화 실패 - 로그인 요청 차단 (Employee: {})", request.getEmployeeId(), e);
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }

        // 3. 사용자 조회
        PlatformUser user = authMapper.findByLoginId(request.getEmployeeId());
        if (user == null) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }

        // 4. 계정 삭제 여부 확인
        if (user.isDeleted()) {
            throw new BusinessException(ErrorCode.ACCOUNT_DELETED);
        }

        // 5. 계정 잠금 여부 확인
        if (user.isLocked()) {
            throw new BusinessException(ErrorCode.ACCOUNT_LOCKED);
        }

        // 6. 비밀번호 검증 (복호화된 평문 비밀번호 사용)
        if (!passwordEncoder.matches(decryptedPassword, user.getPasswordHash())) {
            handleLoginFailure(user);
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }

        // 7. 로그인 성공 — 실패 횟수 초기화
        authMapper.resetFailedLoginCount(user.getId());

        // 8. 토큰 발급
        String accessToken = jwtProvider.generateAccessToken(user.getId(), user.getAgencyId(), user.getLoginId(), user.getRole());
        String refreshToken = jwtProvider.generateRefreshToken(user.getId(), user.getAgencyId(), user.getLoginId(), user.getRole());

        // 9. Redis에 staffId → refreshToken 저장 (8시간 TTL)
        redisTemplate.opsForValue().set(
                getRedisKey(user.getId()),
                refreshToken,
                jwtProvider.getRefreshTokenExpiration() / 1000,
                TimeUnit.SECONDS
        );

        log.info("로그인 성공 - staffId: {}, role: {}", user.getId(), user.getRole());

        return AuthTokenDto.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .role(user.getRole())
                .accessTokenExpiresIn(jwtProvider.getAccessTokenExpiration() / 1000)
                .refreshTokenExpiresIn(jwtProvider.getRefreshTokenExpiration() / 1000)
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
    public AuthTokenDto refresh(String refreshToken) {
        // 1. Refresh Token 유효성 검증
        jwtProvider.validateToken(refreshToken);

        // 2. staffId 추출
        Long staffId = jwtProvider.extractStaffId(refreshToken);
        Long agencyId = jwtProvider.extractAgencyId(refreshToken);
        String loginId = jwtProvider.extractLoginId(refreshToken);
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
        String newAccessToken = jwtProvider.generateAccessToken(staffId, agencyId, loginId, role);
        String newRefreshToken = jwtProvider.generateRefreshToken(staffId, agencyId, loginId, role);

        // 7. Redis 갱신
        redisTemplate.opsForValue().set(
                getRedisKey(staffId),
                newRefreshToken,
                jwtProvider.getRefreshTokenExpiration() / 1000,
                TimeUnit.SECONDS
        );

        return AuthTokenDto.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .role(role)
                .accessTokenExpiresIn(jwtProvider.getAccessTokenExpiration() / 1000)
                .refreshTokenExpiresIn(jwtProvider.getRefreshTokenExpiration() / 1000)
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
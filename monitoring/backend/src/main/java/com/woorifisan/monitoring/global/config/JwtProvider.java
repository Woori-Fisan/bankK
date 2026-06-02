package com.woorifisan.monitoring.global.config;

import com.woorifisan.monitoring.global.exception.BusinessException;
import com.woorifisan.monitoring.global.response.ErrorCode;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * JWT 생성, 검증 및 Redis 기반 상태 관리(RTR, Blacklist)를 전담하는 컴포넌트입니다.
 */
@Slf4j
@Component
public class JwtProvider {

    private final SecretKey secretKey;
    private final long accessTokenExpiration;
    private final long refreshTokenExpiration;
    private final StringRedisTemplate redisTemplate;

    private static final String REDIS_RT_PREFIX = "RT-MONITORING:";
    private static final String REDIS_BLACKLIST_PREFIX = "BLACKLIST:";

    public JwtProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-expiration}") long accessTokenExpiration,
            @Value("${jwt.refresh-expiration}") long refreshTokenExpiration,
            StringRedisTemplate redisTemplate
    ) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpiration = accessTokenExpiration;
        this.refreshTokenExpiration = refreshTokenExpiration;
        this.redisTemplate = redisTemplate;
    }

    // Access Token 생성
    public String generateAccessToken(Long staffId, String loginId) {
        return buildToken(staffId, loginId, accessTokenExpiration);
    }

    // Refresh Token 생성
    public String generateRefreshToken(Long staffId, String loginId) {
        return buildToken(staffId, loginId, refreshTokenExpiration);
    }

    /**
     * Refresh Token Redis 저장
     */
    public void saveRefreshToken(Long userId, String refreshToken) {
        try {
            redisTemplate.opsForValue().set(
                    REDIS_RT_PREFIX + userId,
                    refreshToken,
                    refreshTokenExpiration,
                    TimeUnit.MILLISECONDS
            );
        } catch (Exception e) {
            log.error("[Redis 오류] Refresh Token 저장 실패 - staffId: {}", userId, e);
            throw new BusinessException(ErrorCode.REDIS_CONNECTION_FAILURE);
        }
    }

    /**
     * Redis에서 Refresh Token 조회
     */
    public String getStoredRefreshToken(Long userId) {
        try {
            return redisTemplate.opsForValue().get(REDIS_RT_PREFIX + userId);
        } catch (Exception e) {
            log.error("[Redis 오류] Refresh Token 조회 실패 - staffId: {}", userId, e);
            throw new BusinessException(ErrorCode.REDIS_CONNECTION_FAILURE);
        }
    }

    /**
     * Redis에서 Refresh Token 삭제
     */
    public void deleteRefreshToken(Long userId) {
        try {
            redisTemplate.delete(REDIS_RT_PREFIX + userId);
        } catch (Exception e) {
            log.error("[Redis 오류] Refresh Token 삭제 실패 - staffId: {}", userId, e);
            throw new BusinessException(ErrorCode.REDIS_CONNECTION_FAILURE);
        }
    }

    /**
     * 토큰을 블랙리스트에 등록 (로그아웃 또는 공격 감지 시)
     */
    public void addToBlacklist(String token) {
        try {
            long remainingTime = getRemainingExpiration(token);
            if (remainingTime > 0) {
                redisTemplate.opsForValue().set(
                        REDIS_BLACKLIST_PREFIX + token,
                        "invalidated",
                        remainingTime,
                        TimeUnit.MILLISECONDS
                );
            }
        } catch (Exception e) {
            log.error("[Redis 오류] 블랙리스트 등록 실패", e);
        }
    }

    /**
     * 블랙리스트 여부 확인
     */
    public boolean isBlacklisted(String token) {
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(REDIS_BLACKLIST_PREFIX + token));
        } catch (Exception e) {
            log.error("[Redis 오류] 블랙리스트 확인 실패", e);
            return false;
        }
    }

    public long getAccessTokenExpiration() {
        return accessTokenExpiration;
    }

    public long getRefreshTokenExpiration() {
        return refreshTokenExpiration;
    }

    // 토큰 생성 공통 메서드
    private String buildToken(Long staffId, String loginId, long expiration) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .subject(String.valueOf(staffId))
                .claim("loginId", loginId)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(secretKey)
                .compact();
    }

    /**
     * 토큰에서 모든 정보(Claims) 추출 및 검증 (1회 파싱)
     * Claims는 Map<String, Object>를 상속하므로 유연하게 사용 가능
     */
    public Claims getClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            throw new BusinessException(ErrorCode.EXPIRED_TOKEN);
        } catch (JwtException | IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }
    }

    /**
     * 토큰의 남은 유효 시간을 밀리초 단위로 반환 (블랙리스트 저장용)
     */
    public long getRemainingExpiration(String token) {
        try {
            Claims claims = getClaims(token);
            long now = new Date().getTime();
            return Math.max(0, claims.getExpiration().getTime() - now);
        } catch (BusinessException e) {
            return 0; // 이미 만료되었거나 유효하지 않은 토큰
        } catch (Exception e) {
            log.error("토큰 만료 시간 추출 중 오류 발생", e);
            return 0;
        }
    }
}

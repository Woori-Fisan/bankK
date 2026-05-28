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

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;

    public AuthTokenDto login(LoginRequest request) {
        log.info("[로그인 시도] loginId: {}", request.getLoginId());

        // 1. 사용자 조회
        User user = userMapper.findByLoginId(request.getLoginId());
        if (user == null) {
            log.warn("[로그인 실패] 존재하지 않는 사용자: {}", request.getLoginId());
            throw new BusinessException(ErrorCode.LOGIN_FAILED);
        }

        // 2. 비밀번호 확인
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            log.warn("[로그인 실패] 비밀번호 불일치 - loginId: {}", request.getLoginId());
            throw new BusinessException(ErrorCode.LOGIN_FAILED);
        }

        // 3. 토큰 생성
        String accessToken = jwtProvider.generateAccessToken(user.getId(), user.getLoginId());
        String refreshToken = jwtProvider.generateRefreshToken(user.getId(), user.getLoginId());

        log.info("[로그인 성공] loginId: {}, staffId: {}", user.getLoginId(), user.getId());

        return AuthTokenDto.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .refreshTokenExpiresIn(jwtProvider.getRefreshTokenExpiration() / 1000) // seconds
                .loginId(user.getLoginId())
                .build();
    }

    @Transactional
    public void register(RegisterRequest request) {
        log.info("[회원가입 시도] loginId: {}", request.getLoginId());

        // 1. 중복 확인
        if (userMapper.findByLoginId(request.getLoginId()) != null) {
            log.warn("[회원가입 실패] 중복된 아이디: {}", request.getLoginId());
            throw new BusinessException(ErrorCode.DUPLICATE_LOGIN_ID);
        }

        // 2. 비밀번호 암호화
        String passwordHash = passwordEncoder.encode(request.getPassword());

        // 3. 사용자 저장
        User user = User.of(request.getLoginId(), passwordHash, request.getName());
        userMapper.save(user);

        log.info("[회원가입 성공] loginId: {}, name: {}", user.getLoginId(), user.getName());
    }

    public void logout() {
        log.info("[로그아웃] 로그아웃 요청 처리");
        // JWT 기반은 서버 사이드 세션이 없으므로, 클라이언트에서 토큰을 폐기하도록 유도.
        // 필요 시 Redis 블랙리스트 등을 추가할 수 있음.
    }
}

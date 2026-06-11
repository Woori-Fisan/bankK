package com.woorifisan.monitoring.domain.user.service;

import com.woorifisan.monitoring.domain.user.dto.AuthTokenDto;
import com.woorifisan.monitoring.domain.user.dto.request.LoginRequest;
import com.woorifisan.monitoring.domain.user.dto.request.RegisterRequest;
import com.woorifisan.monitoring.domain.user.mapper.UserMapper;
import com.woorifisan.monitoring.domain.user.model.User;
import com.woorifisan.monitoring.global.config.JwtProvider;
import com.woorifisan.monitoring.global.exception.BusinessException;
import com.woorifisan.monitoring.global.response.ErrorCode;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @InjectMocks
    private UserService userService;

    @Mock
    private UserMapper userMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtProvider jwtProvider;

    @Test
    @DisplayName("로그인_비밀번호가_일치하면_토큰을_발급한다")
    void login_passwordMatches_emitsTokens() {
        // given
        LoginRequest request = new LoginRequest();
        ReflectionTestUtils.setField(request, "loginId", "admin");
        ReflectionTestUtils.setField(request, "password", "securePassword123!");

        User user = User.builder()
                .id(1L)
                .loginId("admin")
                .passwordHash("hashed_pwd")
                .name("관리자")
                .isActive(true)
                .build();

        given(userMapper.findByLoginId("admin")).willReturn(user);
        given(passwordEncoder.matches("securePassword123!", "hashed_pwd")).willReturn(true);
        given(jwtProvider.generateAccessToken(1L, "admin")).willReturn("mock_access_token");
        given(jwtProvider.generateRefreshToken(1L, "admin")).willReturn("mock_refresh_token");
        given(jwtProvider.getAccessTokenExpiration()).willReturn(3600000L);
        given(jwtProvider.getRefreshTokenExpiration()).willReturn(604800000L);

        // when
        AuthTokenDto result = userService.login(request);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getAccessToken()).isEqualTo("mock_access_token");
        assertThat(result.getRefreshToken()).isEqualTo("mock_refresh_token");
        assertThat(result.getLoginId()).isEqualTo("admin");
        verify(jwtProvider, times(1)).saveRefreshToken(1L, "mock_refresh_token");
    }

    @Test
    @DisplayName("로그인_비밀번호가_일치하지_않으면_예외가_발생한다")
    void login_passwordMismatches_throwsException() {
        // given
        LoginRequest request = new LoginRequest();
        ReflectionTestUtils.setField(request, "loginId", "admin");
        ReflectionTestUtils.setField(request, "password", "wrongPassword");

        User user = User.builder()
                .id(1L)
                .loginId("admin")
                .passwordHash("hashed_pwd")
                .name("관리자")
                .isActive(true)
                .build();

        given(userMapper.findByLoginId("admin")).willReturn(user);
        given(passwordEncoder.matches("wrongPassword", "hashed_pwd")).willReturn(false);

        // when & then
        assertThatThrownBy(() -> userService.login(request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.LOGIN_FAILED);
    }

    @Test
    @DisplayName("토큰재발급_유효한_리프레시토큰이면_새로운_토큰쌍을_발급한다")
    void refresh_validToken_reissuesTokens() {
        // given
        String refreshToken = "valid_refresh_token";
        Claims claims = mock(Claims.class);
        given(claims.getSubject()).willReturn("1");
        given(claims.get("loginId", String.class)).willReturn("admin");
        given(jwtProvider.getClaims(refreshToken)).willReturn(claims);

        given(jwtProvider.getStoredRefreshToken(1L)).willReturn(refreshToken);

        User user = User.builder()
                .id(1L)
                .loginId("admin")
                .passwordHash("hashed_pwd")
                .name("관리자")
                .isActive(true)
                .build();
        given(userMapper.findByLoginId("admin")).willReturn(user);

        given(jwtProvider.generateAccessToken(1L, "admin")).willReturn("new_access_token");
        given(jwtProvider.generateRefreshToken(1L, "admin")).willReturn("new_refresh_token");
        given(jwtProvider.getAccessTokenExpiration()).willReturn(3600000L);
        given(jwtProvider.getRefreshTokenExpiration()).willReturn(604800000L);

        // when
        AuthTokenDto result = userService.refresh(refreshToken);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getAccessToken()).isEqualTo("new_access_token");
        assertThat(result.getRefreshToken()).isEqualTo("new_refresh_token");
        verify(jwtProvider, times(1)).saveRefreshToken(1L, "new_refresh_token");
    }

    @Test
    @DisplayName("토큰재발급_이미_로그아웃된_세션이면_예외가_발생한다")
    void refresh_alreadyLoggedOut_throwsException() {
        // given
        String refreshToken = "expired_refresh_token";
        Claims claims = mock(Claims.class);
        given(claims.getSubject()).willReturn("1");
        given(claims.get("loginId", String.class)).willReturn("admin");
        given(jwtProvider.getClaims(refreshToken)).willReturn(claims);

        given(jwtProvider.getStoredRefreshToken(1L)).willReturn(null); // Redis에 없음

        // when & then
        assertThatThrownBy(() -> userService.refresh(refreshToken))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ALREADY_LOGGED_OUT);
    }

    @Test
    @DisplayName("토큰재발급_토큰_재사용_감지_시_기존토큰을_삭제하고_블랙리스트에_등록한다")
    void refresh_tokenReused_deletesTokenAndBlacklists() {
        // given
        String inputRefreshToken = "reused_refresh_token";
        String storedRefreshToken = "stored_different_refresh_token";
        Claims claims = mock(Claims.class);
        given(claims.getSubject()).willReturn("1");
        given(claims.get("loginId", String.class)).willReturn("admin");
        given(jwtProvider.getClaims(inputRefreshToken)).willReturn(claims);

        given(jwtProvider.getStoredRefreshToken(1L)).willReturn(storedRefreshToken);

        // when & then
        assertThatThrownBy(() -> userService.refresh(inputRefreshToken))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.TOKEN_REUSE_DETECTED);

        // RTR 검증: 세션 삭제와 블랙리스트 등록 호출이 정확한 인자값으로 실행되었는지 검증
        verify(jwtProvider, times(1)).deleteRefreshToken(1L);
        verify(jwtProvider, times(1)).addToBlacklist(inputRefreshToken);
        // 새로운 토큰 발급 등 후속 비즈니스 호출은 차단되어야 함
        verify(jwtProvider, never()).generateAccessToken(anyLong(), anyString());
    }

    @Test
    @DisplayName("회원가입_아이디가_중복되지_않으면_계정을_등록한다")
    void register_uniqueLoginId_savesUser() {
        // given
        RegisterRequest request = new RegisterRequest();
        ReflectionTestUtils.setField(request, "loginId", "newUser");
        ReflectionTestUtils.setField(request, "password", "pwd123");
        ReflectionTestUtils.setField(request, "name", "홍길동");

        given(userMapper.findByLoginId("newUser")).willReturn(null);
        given(passwordEncoder.encode("pwd123")).willReturn("encoded_pwd");

        // when
        userService.register(request);

        // then
        verify(userMapper, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("회원가입_아이디가_중복되면_예외가_발생한다")
    void register_duplicateLoginId_throwsException() {
        // given
        RegisterRequest request = new RegisterRequest();
        ReflectionTestUtils.setField(request, "loginId", "admin");
        ReflectionTestUtils.setField(request, "password", "pwd123");
        ReflectionTestUtils.setField(request, "name", "관리자");

        User user = User.builder()
                .id(1L)
                .loginId("admin")
                .passwordHash("hashed_pwd")
                .name("관리자")
                .isActive(true)
                .build();
        given(userMapper.findByLoginId("admin")).willReturn(user);

        // when & then
        assertThatThrownBy(() -> userService.register(request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.DUPLICATE_LOGIN_ID);
        
        verify(userMapper, never()).save(any(User.class));
    }

    @Test
    @DisplayName("로그아웃_요청_시_세션을_무효화하고_액세스토큰을_블랙리스트에_등록한다")
    void logout_validSession_invalidatesSessionAndBlacklistsAccessToken() {
        // given
        Long userId = 1L;
        String refreshToken = "refresh_token";
        String accessToken = "access_token";

        // when
        userService.logout(userId, refreshToken, accessToken);

        // then
        verify(jwtProvider, times(1)).deleteRefreshToken(userId);
        verify(jwtProvider, times(1)).addToBlacklist(accessToken);
    }

    @Test
    @DisplayName("로그아웃_사용자ID가_없고_리프레시토큰이_존재하면_토큰에서_ID를_추출하여_세션을_무효화한다")
    void logout_noUserIdButValidRefreshToken_extractsIdAndInvalidates() {
        // given
        String refreshToken = "valid_refresh_token";
        String accessToken = "access_token";

        Claims claims = mock(Claims.class);
        given(claims.getSubject()).willReturn("100");
        given(jwtProvider.getClaims(refreshToken)).willReturn(claims);

        // when
        userService.logout(null, refreshToken, accessToken);

        // then
        verify(jwtProvider, times(1)).deleteRefreshToken(100L);
        verify(jwtProvider, times(1)).addToBlacklist(accessToken);
    }

    @Test
    @DisplayName("로그아웃_사용자ID가_없고_유효하지_않은_리프레시토큰이면_세션무효화를_스킵하고_액세스토큰만_블랙리스트에_등록한다")
    void logout_noUserIdAndInvalidRefreshToken_skipsInvalidationAndBlacklistsAccess() {
        // given
        String refreshToken = "invalid_refresh_token";
        String accessToken = "access_token";

        given(jwtProvider.getClaims(refreshToken)).willThrow(new BusinessException(ErrorCode.INVALID_TOKEN));

        // when
        userService.logout(null, refreshToken, accessToken);

        // then
        verify(jwtProvider, never()).deleteRefreshToken(anyLong());
        verify(jwtProvider, times(1)).addToBlacklist(accessToken);
    }
}

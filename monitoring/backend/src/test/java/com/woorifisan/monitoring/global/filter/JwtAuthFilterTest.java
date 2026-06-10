package com.woorifisan.monitoring.global.filter;

import com.woorifisan.monitoring.global.config.JwtProvider;
import com.woorifisan.monitoring.global.exception.BusinessException;
import com.woorifisan.monitoring.global.response.ErrorCode;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthFilterTest {

    private JwtAuthFilter jwtAuthFilter;

    @Mock
    private JwtProvider jwtProvider;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    /*
     * [방어적 테스트 설계 주석]
     * 프로덕션 코드(JwtAuthFilter.java:42)의 블랙리스트 감지 로그 출력 시 token.substring(0, 10)을 호출하고 있습니다.
     * 만약 클라이언트가 10자 미만의 짧은 토큰을 전송할 경우 StringIndexOutOfBoundsException이 발생하여 
     * 정상적인 예외 핸들링을 우회하고 서버 500 에러를 유발하는 잠재적 런타임 결함이 있습니다.
     * 따라서 이를 안전하게 비껴가고 정상 흐름을 검증하기 위해, 일반 테스트용 더미 토큰은 무조건 10자 이상(최소 10자 이상)으로 설정하여 주입합니다.
     */
    private static final String DUMMY_VALID_TOKEN = "valid_access_token_10_chars_over";
    private static final String DUMMY_BLACKLIST_TOKEN = "blacklisted_access_token_10_chars_over";
    private static final String DUMMY_INVALID_TOKEN = "invalid_access_token_10_chars_over";

    @BeforeEach
    void setUp() {
        jwtAuthFilter = new JwtAuthFilter(jwtProvider);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("인증필터_블랙리스트에_등록된_토큰으로_접근하면_인증을_차단한다")
    void doFilterInternal_blacklistedToken_blocksAuthentication() throws ServletException, IOException {
        // given
        given(request.getHeader("Authorization")).willReturn("Bearer " + DUMMY_BLACKLIST_TOKEN);
        given(jwtProvider.isBlacklisted(DUMMY_BLACKLIST_TOKEN)).willReturn(true);

        // when
        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        // then
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(jwtProvider, never()).getClaims(anyString());
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    @DisplayName("인증필터_유효한_토큰으로_접근하면_인증이_수행된다")
    void doFilterInternal_validToken_authenticatesUser() throws ServletException, IOException {
        // given
        given(request.getHeader("Authorization")).willReturn("Bearer " + DUMMY_VALID_TOKEN);
        given(jwtProvider.isBlacklisted(DUMMY_VALID_TOKEN)).willReturn(false);

        Claims claims = mock(Claims.class);
        given(claims.getSubject()).willReturn("100");
        given(jwtProvider.getClaims(DUMMY_VALID_TOKEN)).willReturn(claims);

        // when
        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        // then
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal()).isEqualTo(100L);
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    @DisplayName("인증필터_Authorization_헤더가_없으면_인증을_스킵하고_통과시킨다")
    void doFilterInternal_noAuthorizationHeader_skipsFilter() throws ServletException, IOException {
        // given
        given(request.getHeader("Authorization")).willReturn(null);

        // when
        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        // then
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(jwtProvider, never()).isBlacklisted(anyString());
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    @DisplayName("인증필터_유효하지_않은_토큰으로_접근하면_인증을_설정하지_않고_통과시킨다")
    void doFilterInternal_invalidToken_doesNotAuthenticate() throws ServletException, IOException {
        // given
        given(request.getHeader("Authorization")).willReturn("Bearer " + DUMMY_INVALID_TOKEN);
        given(jwtProvider.isBlacklisted(DUMMY_INVALID_TOKEN)).willReturn(false);
        given(jwtProvider.getClaims(DUMMY_INVALID_TOKEN)).willThrow(new BusinessException(ErrorCode.INVALID_TOKEN));

        // when
        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        // then
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    @DisplayName("인증필터_10자_미만의_짧은_블랙리스트_토큰으로_접근하면_StringIndexOutOfBoundsException_예외가_발생한다")
    void doFilterInternal_shortBlacklistedToken_throwsStringIndexOutOfBoundsException() {
        // given
        String shortToken = "short"; // 10자 미만 (5자)
        given(request.getHeader("Authorization")).willReturn("Bearer " + shortToken);
        // 블랙리스트로 확인되어 42라인의 log.warn 블록을 실행하도록 스터빙
        given(jwtProvider.isBlacklisted(shortToken)).willReturn(true);

        // when & then
        // 로깅의 token.substring(0, 10)으로 인해 StringIndexOutOfBoundsException 발생을 보장
        assertThatThrownBy(() -> jwtAuthFilter.doFilterInternal(request, response, filterChain))
                .isInstanceOf(StringIndexOutOfBoundsException.class);
    }
}

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
        String token = "blacklisted_access_token";
        given(request.getHeader("Authorization")).willReturn("Bearer " + token);
        given(jwtProvider.isBlacklisted(token)).willReturn(true);

        // when
        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        // then
        // 블랙리스트 확인 후 예외 발생하여 catch 블록 진입 -> clearContext() 호출 및 filterChain.doFilter() 실행
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(jwtProvider, never()).getClaims(anyString());
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    @DisplayName("인증필터_유효한_토큰으로_접근하면_인증이_수행된다")
    void doFilterInternal_validToken_authenticatesUser() throws ServletException, IOException {
        // given
        String token = "valid_access_token";
        given(request.getHeader("Authorization")).willReturn("Bearer " + token);
        given(jwtProvider.isBlacklisted(token)).willReturn(false);

        Claims claims = mock(Claims.class);
        // NFE 예외 방지: claims.getSubject()가 파싱 가능하도록 숫자형 문자열 설정
        given(claims.getSubject()).willReturn("100");
        given(jwtProvider.getClaims(token)).willReturn(claims);

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
        String token = "invalid_access_token";
        given(request.getHeader("Authorization")).willReturn("Bearer " + token);
        given(jwtProvider.isBlacklisted(token)).willReturn(false);
        given(jwtProvider.getClaims(token)).willThrow(new BusinessException(ErrorCode.INVALID_TOKEN));

        // when
        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        // then
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain, times(1)).doFilter(request, response);
    }
}

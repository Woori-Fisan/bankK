package com.woorifisan.platform.global.filter;

import com.woorifisan.platform.global.config.JwtProvider;
import com.woorifisan.platform.global.exception.BusinessException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

@Slf4j
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtProvider jwtProvider;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String token = extractToken(request);

        if (token != null) {
            try {
                jwtProvider.validateToken(token);

                Long staffId  = jwtProvider.extractStaffId(token);
                Long agencyId = jwtProvider.extractAgencyId(token);
                String role   = jwtProvider.extractRole(token);

                MDC.put("staffId",  String.valueOf(staffId));
                MDC.put("agencyCode", String.valueOf(agencyId));

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                staffId,
                                null,
                                List.of(new SimpleGrantedAuthority("ROLE_" + role))
                        );

                SecurityContextHolder.getContext().setAuthentication(authentication);

            } catch (BusinessException e) {
                log.warn("JWT 인증 실패 - {}", e.getMessage());
                SecurityContextHolder.clearContext();
            }
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove("staffId");
            MDC.remove("agencyCode");
        }
    }

    // Authorization 헤더에서 Bearer 토큰 추출
    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(header) && header.startsWith(BEARER_PREFIX)) {
            return header.substring(BEARER_PREFIX.length());
        }
        return null;
    }
}
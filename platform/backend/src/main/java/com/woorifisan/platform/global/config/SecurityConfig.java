package com.woorifisan.platform.global.config;

import com.woorifisan.platform.global.filter.JwtAuthFilter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer.FrameOptionsConfig;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy;
import org.springframework.web.servlet.HandlerExceptionResolver;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity  // @PreAuthorize 사용 위해 추가
public class SecurityConfig {

    private final JwtProvider jwtProvider;
    private final HandlerExceptionResolver exceptionResolver;

    public SecurityConfig(JwtProvider jwtProvider, @Qualifier("handlerExceptionResolver") HandlerExceptionResolver exceptionResolver) {
        this.jwtProvider = jwtProvider;
        this.exceptionResolver = exceptionResolver;
    }

    // 인증 없이 접근 가능한 경로
    private static final String[] PUBLIC_URLS = {
            "/api/v1/auth/login",
            "/api/v1/auth/public-key",
            "/api/v1/auth/refresh",
            "/actuator/health",
            "/actuator/prometheus",
            "/swagger-ui.html",
            "/swagger-ui/**",
            "/v3/api-docs/**",
            // 테스트 용 임시 통과 URL -> 플랫폼 개발 완료 시 삭제
            "/api/v1/keys/public",
            "/api/rag/**",
            "/api/v1/loan/**"
    };

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // CORS 설정 적용 (WebMvcConfig 설정을 따름)
                .cors(org.springframework.security.config.Customizer.withDefaults())
                // JWT 사용하므로 세션 미사용
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // CSRF 비활성화 (REST API)
                .csrf(AbstractHttpConfigurer::disable)
                // 보안 헤더
                .headers(headers -> headers
                        .contentSecurityPolicy(csp -> csp.policyDirectives(
                                "default-src 'self'; " +
                                        "script-src 'self'; " +
                                        "style-src 'self' 'unsafe-inline'; " + // CSS는 같은 출처 + 인라인 허용 (React 컴포넌트 inline style 때문에 필요)
                                        "img-src 'self' data:; " + // 이미지는 같은 출처 + Base64 Data URL
                                        "font-src 'self'; " +
                                        "connect-src 'self'; " +
                                        "object-src 'none'; " +
                                        "base-uri 'self'; " +
                                        "form-action 'self'"
                        ))
                        // 이 앱이 다른 사이트의 iframe에 삽입되는 것을 차단 -> Clickjacking 방어
                        .frameOptions(FrameOptionsConfig::deny)
                        // 다른 사이트로 이동할 때 Referer 헤더를 어디까지 보낼지 결정 (다른 출처 → 도메인만 전송)
                        .referrerPolicy(referrer -> referrer
                                .policy(ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN)
                        )
                        // 브라우저 기능 접근 권한을 명시적으로 차단
                        .permissionsPolicyHeader(permissions -> permissions
                                .policy("camera=(), microphone=(), geolocation=()")
                        )
                )
                // 경로별 접근 권한 설정
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC_URLS).permitAll()
                        .requestMatchers("/api/v1/admin/**").hasRole("AGENCY_ADMIN")
                        .anyRequest().authenticated()
                )
                // Security 예외를 GlobalExceptionHandler로 전달하기 위한 설정
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, authException) ->
                                exceptionResolver.resolveException(request, response, null, authException))
                        .accessDeniedHandler((request, response, accessDeniedException) ->
                                exceptionResolver.resolveException(request, response, null, accessDeniedException))
                )
                // JwtAuthFilter를 UsernamePasswordAuthenticationFilter 앞에 등록
                .addFilterBefore(jwtAuthFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public JwtAuthFilter jwtAuthFilter() {
        return new JwtAuthFilter(jwtProvider);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        return new InMemoryUserDetailsManager();
    }
}
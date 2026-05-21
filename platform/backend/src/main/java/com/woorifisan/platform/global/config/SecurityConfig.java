package com.woorifisan.platform.global.config;

import com.woorifisan.platform.global.filter.JwtAuthFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity  // @PreAuthorize 사용 위해 추가
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtProvider jwtProvider;

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
            "/api/v1/bank/inquiry/**",
            "/api/v1/bank/transfer/**",
            "/api/v1/bank/withdrawals/**",
            "/api/v1/loan/**",
            "/api/v1/banks",
            "/api/v1/crypto/public-key",
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
                // 경로별 접근 권한 설정
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC_URLS).permitAll()
                        .requestMatchers("/api/v1/admin/**").hasRole("AGENCY_ADMIN")
                        .anyRequest().authenticated()
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
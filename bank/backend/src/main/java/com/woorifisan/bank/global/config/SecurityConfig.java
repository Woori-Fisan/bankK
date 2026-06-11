package com.woorifisan.bank.global.config;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity  // @PreAuthorize 사용 위해 추가
@RequiredArgsConstructor
public class SecurityConfig {

    // 인증 없이 접근 가능한 경로
    private static final String[] PUBLIC_URLS = {
            "/actuator/health",
            "/actuator/prometheus",
            "/swagger-ui.html",
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/api/v1/baas/keys/**"
    };

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // CORS 설정 적용 (WebMvcConfig 설정을 따름)
                .cors(org.springframework.security.config.Customizer.withDefaults())
                // REST API이므로 CSRF 비활성화 및 무상태 세션 정책 설정
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                
                // x509 클라이언트 인증 설정
                .x509(x509 -> x509
                        .subjectPrincipalRegex("CN=(.*?)(?:,|$)")
                )

                // 요청 경로별 권한 설정
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC_URLS).permitAll() // 헬스체크, Swagger 등 허용
                        .anyRequest().authenticated()             // 그 외 모든 요청은 인증 필요 (mTLS 포함)
                );

        return http.build();
    }


    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService(@Lazy BankNetworkConfig bankNetworkConfig) {
        List<UserDetails> users = new ArrayList<>();
        Set<String> usernames = new HashSet<>();

        // 1. 플랫폼 클라이언트 등록
        String platformUser = "platformClient";
        users.add(User.withUsername(platformUser)
                .password(passwordEncoder().encode("mtls-password"))
                .roles("PLATFORM")
                .build());
        usernames.add(platformUser);

        // 2. bank-network 설정에서 은행 IP(CN)들을 중복 없이 추출하여 등록
        if (bankNetworkConfig != null && bankNetworkConfig.getBanks() != null) {
            bankNetworkConfig.getBanks().forEach((code, property) -> {
                try {
                    String host = URI.create(property.getBaseUrl()).getHost();
                    if (host != null && !usernames.contains(host)) {
                        users.add(User.withUsername(host)
                                .password(passwordEncoder().encode("mtls-password"))
                                .roles("BANK")
                                .build());
                        usernames.add(host); // 중복 등록 방지
                    }
                } catch (Exception e) {
                    // 잘못된 URL 형식은 무시
                }
            });
        }

        return new InMemoryUserDetailsManager(users);
    }
}
package com.woorifisan.bank.global.config;

import java.io.FileInputStream;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.time.Duration;
import java.net.http.HttpClient;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${ssl.server.keystore.path}")
    private String keystorePath;

    @Value("${ssl.server.keystore.password}")
    private String keystorePassword;

    @Value("${ssl.truststore.path}")
    private String truststorePath;

    @Value("${ssl.truststore.password}")
    private String truststorePassword;

    @Bean
    public RestTemplate restTemplate() {
        try {
            // 1. KeyStore 로드 (클라이언트 인증서)
            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            try (InputStream is = new FileInputStream(keystorePath.replace("file:", ""))) {
                keyStore.load(is, keystorePassword.toCharArray());
            }

            // 2. TrustStore 로드 (CA 인증서)
            KeyStore trustStore = KeyStore.getInstance("PKCS12");
            try (InputStream is = new FileInputStream(truststorePath.replace("file:", ""))) {
                trustStore.load(is, truststorePassword.toCharArray());
            }

            // 3. SSLContext 생성 (표준 API 사용)
            KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(keyStore, keystorePassword.toCharArray());

            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(trustStore);

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), new SecureRandom());

            // 4. HttpClient 생성 (Java 11+ HttpClient)
            HttpClient httpClient = HttpClient.newBuilder()
                    .sslContext(sslContext)
                    .connectTimeout(Duration.ofSeconds(3))
                    .build();

            // 5. RestTemplate 생성
            JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
            // setReadTimeout은 Spring 6.1+ 에서 지원, 그 전에는 HttpClient 레벨에서 제어 권장
            
            return new RestTemplate(factory);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create secure RestTemplate", e);
        }
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}

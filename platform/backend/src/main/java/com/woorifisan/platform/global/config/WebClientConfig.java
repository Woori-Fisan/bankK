package com.woorifisan.platform.global.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.timeout.ReadTimeoutHandler;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.KeyStore;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

/**
 * 은행 서버와의 통신을 위한 WebClient 설정
 */
@Configuration
public class WebClientConfig {

    @Value("${ssl.keystore.path}")
    private String keystorePath;

    @Value("${ssl.keystore.password}")
    private String keystorePassword;

    @Value("${ssl.truststore.path}")
    private String truststorePath;

    @Value("${ssl.truststore.password}")
    private String truststorePassword;

    // 세션 락 TTL(45s)보다 짧게 설정하여 락 만료 전에 타임아웃이 발생하도록 보장
    private static final int CONNECT_TIMEOUT_MS = 5_000;
    private static final int READ_TIMEOUT_SECONDS = 25;

    @Bean
    public WebClient bankWebClient(WebClient.Builder builder) {
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

            // 3. SSL 설정
            KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(keyStore, keystorePassword.toCharArray());

            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(trustStore);

            SslContext sslContext = SslContextBuilder.forClient()
                    .keyManager(kmf)
                    .trustManager(tmf)
                    .build();

            HttpClient httpClient = HttpClient.create()
                    .secure(ssl -> ssl.sslContext(sslContext))
                    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, CONNECT_TIMEOUT_MS)
                    .responseTimeout(Duration.ofSeconds(READ_TIMEOUT_SECONDS))
                    .doOnConnected(conn ->
                            conn.addHandlerLast(new ReadTimeoutHandler(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)));

            return builder
                    .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .clientConnector(new ReactorClientHttpConnector(httpClient))
                    .build();
        } catch (Exception e) {
            throw new RuntimeException("Failed to create secure WebClient", e);
        }
    }
}

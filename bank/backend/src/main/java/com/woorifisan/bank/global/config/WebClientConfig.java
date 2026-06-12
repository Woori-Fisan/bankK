package com.woorifisan.bank.global.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.timeout.ReadTimeoutHandler;
import java.io.InputStream;
import java.security.KeyStore;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

/**
 * 타행 통신용 WebClient 설정 (mTLS 적용)
 * 이 은행의 서버 인증서를 클라이언트 인증서로 사용하여 상호 인증합니다.
 */
@Configuration
public class WebClientConfig {

    // 이 은행의 서버 인증서 — 타행 요청 시 클라이언트 신원 증명에 사용
    @Value("${ssl.server.keystore.path}")
    private Resource keystoreResource;

    @Value("${ssl.server.keystore.password}")
    private String keystorePassword;

    // 타행 CA 인증서 검증용 트러스트스토어
    @Value("${ssl.truststore.path}")
    private Resource truststoreResource;

    @Value("${ssl.truststore.password}")
    private String truststorePassword;

    private static final int CONNECT_TIMEOUT_MS = 3_000;
    private static final int READ_TIMEOUT_SECONDS = 5;

    @Bean("bankToBankWebClient")
    public WebClient bankToBankWebClient(WebClient.Builder builder) {
        try {
            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            try (InputStream is = keystoreResource.getInputStream()) {
                keyStore.load(is, keystorePassword.toCharArray());
            }

            KeyStore trustStore = KeyStore.getInstance("PKCS12");
            try (InputStream is = truststoreResource.getInputStream()) {
                trustStore.load(is, truststorePassword.toCharArray());
            }

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
            throw new RuntimeException("타행 통신용 WebClient(mTLS) 생성 실패", e);
        }
    }
}
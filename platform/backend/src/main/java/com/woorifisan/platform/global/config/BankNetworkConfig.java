package com.woorifisan.platform.global.config;

import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 은행별 네트워크 설정을 관리하는 설정 클래스입니다.
 * application.yml의 'bank.urls' 설정을 매핑하여 각 은행의 베이스 URL과 기능별 엔드포인트를 관리합니다.
 */
@Configuration
@ConfigurationProperties(prefix = "bank")
@Getter
@Setter
public class BankNetworkConfig {

    /**
     * 은행 코드(Key)와 해당 은행의 상세 설정(Value) 맵
     */
    private Map<String, BankProperty> urls = new HashMap<>();

    @Getter
    @Setter
    public static class BankProperty {
        /**
         * 해당 은행의 BaaS API 베이스 URL
         */
        private String baseUrl;

        /**
         * 기능별 상세 엔드포인트 맵 (예: balance -> /accounts/balance)
         */
        private Map<String, String> endpoints = new HashMap<>();

        /**
         * 특정 기능의 전체 URL을 조합하여 반환합니다.
         *
         * @param key 기능 식별 키 (예: "balance", "withdraw")
         * @return 조합된 전체 URL (baseUrl + endpoint)
         */
        public String getUrl(String key) {
            String endpoint = endpoints.getOrDefault(key, "");
            return baseUrl + (endpoint.startsWith("/") ? endpoint : "/" + endpoint);
        }
    }

    /**
     * 은행 코드를 사용하여 해당 은행의 설정을 조회합니다.
     *
     * @param bankCode 조회할 은행 코드 (예: "020")
     * @return 해당 은행의 BankConfig 객체, 존재하지 않을 경우 null
     */
    public BankProperty getBankProperty(String bankCode) {
        return urls.get(bankCode);
    }
}

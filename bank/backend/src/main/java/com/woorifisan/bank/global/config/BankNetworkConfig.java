package com.woorifisan.bank.global.config;

import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 은행 간 네트워크 설정을 관리하는 클래스
 */
@Configuration
@ConfigurationProperties(prefix = "bank-network")
@Getter
@Setter
public class BankNetworkConfig {

    private Map<String, BankProperty> banks = new HashMap<>();

    @Getter
    @Setter
    public static class BankProperty {
        private String baseUrl;

        public String getInternalDepositUrl() {
            return baseUrl + "/api/v1/baas/transfer/internal/deposit";
        }

        public String getStatusQueryUrl(String txId) {
            return baseUrl + "/api/v1/baas/transfer/status/" + txId;
        }
    }

    public BankProperty getBankProperty(String bankCode) {
        return banks.get(bankCode);
    }
}

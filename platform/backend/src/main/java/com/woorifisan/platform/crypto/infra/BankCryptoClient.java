package com.woorifisan.platform.crypto.infra;

import com.woorifisan.platform.crypto.dto.BankApiResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class BankCryptoClient {

//    private final WebClient bankWebClient;
//
//    public BankCryptoClient(@Qualifier("bankWebClient") WebClient bankWebClient) {
//        this.bankWebClient = bankWebClient;
//    }
//
//    public BankApiResponse fetchPublicKeyFromBank(String bankCode) {
//        // 은행 코어 백엔드 API 호출 뼈대 로직
//        // 실제 은행 서버의 URL 및 Endpoint 맞춰 수정 필요 (현재는 임시 URL)
//        return bankWebClient.get()
//                .uri(uriBuilder -> uriBuilder
//                        .scheme("http")
//                        .host("localhost") // 임시 호스트
//                        .port(8080)        // 임시 포트
//                        .path("/api/crypto/public-key") // 임시 경로
//                        .queryParam("bankCode", bankCode)
//                        .build())
//                .retrieve()
//                .bodyToMono(BankApiResponse.class)
//                .block(); // 동기적으로 결과 대기
//    }

    public BankApiResponse fetchPublicKeyFromBank(String bankCode) {
        // 더미 데이터 생성 로직
        BankApiResponse dummyResponse = new BankApiResponse();
        dummyResponse.setBankCode(bankCode);
        dummyResponse.setKeyId("v1_key");
        dummyResponse.setPublicKey("MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA-DUMMY-KEY-FOR-TESTING");

        System.out.println("⚠️ [DUMMY] 은행 공개키 요청 호출됨 (BankCode: " + bankCode + ")");

        return dummyResponse;
    }
}

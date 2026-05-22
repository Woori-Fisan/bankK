package com.woorifisan.platform.domain.bank.external.client;

import com.woorifisan.platform.domain.bank.dto.response.BalanceInquiryResponse;
import com.woorifisan.platform.domain.bank.external.dto.BankBalanceInquiryRequest;
import com.woorifisan.platform.global.config.BankNetworkConfig;
import com.woorifisan.platform.global.config.BankNetworkConfig.BankProperty;
import com.woorifisan.platform.global.exception.BusinessException;
import com.woorifisan.platform.global.response.ApiResponse;
import com.woorifisan.platform.global.response.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * 외부 은행 코어 시스템과의 통신을 전담하는 클라이언트 클래스입니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BankExternalClient {

    private final WebClient webClient;
    private final BankNetworkConfig bankNetworkConfig;

    /**
     * 특정 은행의 잔액 조회 API를 호출합니다.
     *
     * @param bankCode 은행 코드
     * @param request  은행 전용 잔액 조회 요청 DTO
     * @return 잔액 조회 결과 응답
     */
    public BalanceInquiryResponse fetchBalance(String bankCode, BankBalanceInquiryRequest request) {
        BankProperty bankProperty = bankNetworkConfig.getBankProperty(bankCode);

        if (bankProperty == null) {
            log.error("지원하지 않는 은행 코드입니다: {}", bankCode);
            throw new BusinessException(ErrorCode.BANK_NOT_FOUND);
        }

        String url = bankProperty.getUrl("balance");
        log.info("외부 은행 API 호출 [잔액조회] - URL: {}, 은행코드: {}", url, bankCode);

        try {
            ApiResponse<BalanceInquiryResponse> response = webClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<ApiResponse<BalanceInquiryResponse>>() {})
                    .block();

            if (response != null && response.isSuccess()) {
                return response.getData();
            } else {
                String errorCode = (response != null && response.getError() != null) ? response.getError().getCode() : "UNKNOWN_ERROR";
                log.error("외부 은행 API 응답 에러 - 코드: {}", errorCode);
                throw new BusinessException(ErrorCode.BANK_API_ERROR);
            }
        } catch (Exception e) {
            log.error("외부 은행 API 통신 중 오류 발생", e);
            throw new BusinessException(ErrorCode.BANK_API_ERROR);
        }
    }
}

package com.woorifisan.platform.domain.bank.external.client;

import com.woorifisan.platform.crypto.dto.response.BankRsaKeyResponse;
import com.woorifisan.platform.domain.bank.dto.response.BalanceInquiryResponse;
import com.woorifisan.platform.domain.bank.dto.response.HistoryInquiryResponse;
import com.woorifisan.platform.domain.bank.dto.response.TransferResponse;
import com.woorifisan.platform.domain.bank.dto.response.TransferRecipientResponse;
import com.woorifisan.platform.domain.bank.external.dto.BankBalanceInquiryRequest;
import com.woorifisan.platform.domain.bank.external.dto.BankHistoryInquiryRequest;
import com.woorifisan.platform.domain.bank.external.dto.BankRecipientRequest;
import com.woorifisan.platform.domain.bank.external.dto.BankTransferResponse;
import com.woorifisan.platform.domain.bank.external.dto.BankTransferWithdrawRequest;
import com.woorifisan.platform.domain.bank.external.dto.BankWithdrawalRequest;
import com.woorifisan.platform.global.aop.annotation.ExcludeLogging;
import com.woorifisan.platform.global.config.BankNetworkConfig;
import com.woorifisan.platform.global.config.BankNetworkConfig.BankProperty;
import com.woorifisan.platform.global.exception.BankCoreException;
import com.woorifisan.platform.global.exception.BusinessException;
import com.woorifisan.platform.global.response.ApiResponse;
import com.woorifisan.platform.global.response.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

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
     * 특정 은행의 RSA 공개키를 조회합니다.
     */
    @ExcludeLogging
    public BankRsaKeyResponse fetchPublicKey(String bankCode) {
        BankProperty bankProperty = bankNetworkConfig.getBankProperty(bankCode);
        if (bankProperty == null) throw new BusinessException(ErrorCode.BANK_NOT_FOUND);

        String url = bankProperty.getUrl("public-key");
        log.info("외부 은행 API 호출 [RSA공개키조회] - URL: {}, 은행코드: {}", url, bankCode);

        return getRequest(url, new ParameterizedTypeReference<ApiResponse<BankRsaKeyResponse>>() {}, bankCode);
    }

    /**
     * 특정 은행의 수취인 조회 API를 호출합니다.
     */
    public TransferRecipientResponse fetchRecipient(String bankCode, BankRecipientRequest request) {
        BankProperty bankProperty = bankNetworkConfig.getBankProperty(bankCode);
        if (bankProperty == null) throw new BusinessException(ErrorCode.BANK_NOT_FOUND);

        String url = bankProperty.getUrl("recipient");

        return postRequest(url, request, new ParameterizedTypeReference<ApiResponse<TransferRecipientResponse>>() {}, bankCode);
    }



    /**
     * 특정 은행의 통합 이체 API를 호출합니다.
     */
    public BankTransferResponse fetchTransferExecute(String bankCode, BankTransferWithdrawRequest request) {
        BankProperty bankProperty = bankNetworkConfig.getBankProperty(bankCode);
        if (bankProperty == null) throw new BusinessException(ErrorCode.BANK_NOT_FOUND);

        String url = bankProperty.getUrl("execute");
        log.info("외부 은행 API 호출 [통합이체] - URL: {}, 은행코드: {}", url, bankCode);

        return postRequest(url, request, new ParameterizedTypeReference<ApiResponse<BankTransferResponse>>() {}, bankCode);
    }



    /**
     * 특정 은행의 잔액 조회 API를 호출합니다.
     *
     * @param bankCode 은행 코드
     * @param request  은행 전용 잔액 조회 요청 DTO
     * @return 잔액 조회 결과 응답
     */
    public BalanceInquiryResponse fetchBalance(String bankCode, BankBalanceInquiryRequest request) {
        BankProperty bankProperty = bankNetworkConfig.getBankProperty(bankCode);
        if (bankProperty == null) throw new BusinessException(ErrorCode.BANK_NOT_FOUND);

        String url = bankProperty.getUrl("balance");

        return postRequest(url, request, new ParameterizedTypeReference<ApiResponse<BalanceInquiryResponse>>() {}, bankCode);
    }

    /**
     * 특정 은행의 현금 출금 API를 호출합니다.
     */
    public TransferResponse withdraw(String bankCode, BankWithdrawalRequest request) {
        BankProperty bankProperty = bankNetworkConfig.getBankProperty(bankCode);
        if (bankProperty == null) throw new BusinessException(ErrorCode.BANK_NOT_FOUND);

        String url = bankProperty.getUrl("withdrawal");

        return postRequest(url, request, new ParameterizedTypeReference<ApiResponse<TransferResponse>>() {}, bankCode);
    }

    /**
     * 특정 은행의 거래 내역 조회 API를 호출합니다.
     */
    public HistoryInquiryResponse fetchHistory(String bankCode, BankHistoryInquiryRequest request) {
        BankProperty bankProperty = bankNetworkConfig.getBankProperty(bankCode);
        if (bankProperty == null) throw new BusinessException(ErrorCode.BANK_NOT_FOUND);

        String url = bankProperty.getUrl("history");

        return postRequest(url, request, new ParameterizedTypeReference<ApiResponse<HistoryInquiryResponse>>() {}, bankCode);
    }
    
    /**
     * 공통 GET 요청 처리 메서드
     */
    private <T> T getRequest(String url, ParameterizedTypeReference<ApiResponse<T>> responseType, String bankCode) {
        try {
            String traceId = MDC.get("traceId");
            ApiResponse<T> response = webClient.get()
                    .uri(url)
                    .headers(h -> { if (StringUtils.hasText(traceId)) h.set("X-Trace-Id", traceId); })
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, clientResponse ->
                            clientResponse.bodyToMono(responseType)
                                    .flatMap(errorBody -> {
                                        String bankErrorCode = (errorBody.getError() != null) ? errorBody.getError().getCode() : "UNKNOWN";
                                        return Mono.<Throwable>error(new BusinessException(mapToInternalErrorCode(bankErrorCode)));
                                    })
                                    .switchIfEmpty(Mono.<Throwable>error(new BusinessException(ErrorCode.BANK_API_ERROR)))
                    )
                    .bodyToMono(responseType)
                    .block();

            if (response == null || response.getData() == null) {
                log.error("외부 은행 API 응답 바디 또는 데이터가 null입니다. 은행코드: {}", bankCode);
                throw new BusinessException(ErrorCode.BANK_API_ERROR);
            }

            return response.getData();
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("외부 은행 API 통신 중 오류 발생", e);
            throw new BusinessException(ErrorCode.BANK_API_ERROR);
        }
    }

    /**
     * 공통 POST 요청 처리 메서드
     */
    private <T, R> T postRequest(String url, R requestBody, ParameterizedTypeReference<ApiResponse<T>> responseType, String bankCode) {
        try {
            String traceId = MDC.get("traceId");
            ApiResponse<T> response = webClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .headers(h -> { if (StringUtils.hasText(traceId)) h.set("X-Trace-Id", traceId); })
                    .bodyValue(requestBody)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, clientResponse ->
                            clientResponse.bodyToMono(responseType)
                                    .flatMap(errorBody -> {
                                        String bankErrorCode    = (errorBody.getError() != null) ? errorBody.getError().getCode()    : "UNKNOWN";
                                        String bankErrorMessage = (errorBody.getError() != null) ? errorBody.getError().getMessage() : "UNKNOWN";
                                        int    bankHttpStatus   = clientResponse.statusCode().value();
                                        return Mono.<Throwable>error(new BankCoreException(mapToInternalErrorCode(bankErrorCode), bankErrorCode, bankErrorMessage, bankHttpStatus));
                                    })
                                    .switchIfEmpty(Mono.defer(() -> {
                                        int bankHttpStatus = clientResponse.statusCode().value();
                                        return Mono.error(new BankCoreException(ErrorCode.BANK_API_ERROR, "UNKNOWN", "Empty error response", bankHttpStatus));
                                    }))
                    )
                    .bodyToMono(responseType)
                    .block();

            if (response == null || response.getData() == null) {
                throw new BusinessException(ErrorCode.BANK_API_ERROR);
            }

            return response.getData();
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.BANK_API_ERROR);
        }
    }

    private ErrorCode mapToInternalErrorCode(String bankErrorCode) {
        return switch (bankErrorCode) {
            case "ACC_001", "BANK_001", "USER_001" -> ErrorCode.INQUIRY_ACCOUNT_NOTFOUND;
            case "ACC_002" -> ErrorCode.TRANSFER_WITHDRAW_AMOUNT_FAULT;
            case "ACC_003" -> ErrorCode.TRANSFER_WITHDRAW_ACCOUNT_FAULT;
            case "ACC_004" -> ErrorCode.TRANSFER_WITHDRAW_ACCOUNT_STATUS_FAULT;
            case "ACC_005" -> ErrorCode.DUPLICATE_REQUEST;
            case "ACC_006" -> ErrorCode.BANK_PW_ERROR;
            case "ERR_001" -> ErrorCode.INVALID_INPUT;
            case "ERR_002" -> ErrorCode.INTERNAL_SERVER_ERROR;
            default -> ErrorCode.BANK_API_ERROR;
        };
    }
}

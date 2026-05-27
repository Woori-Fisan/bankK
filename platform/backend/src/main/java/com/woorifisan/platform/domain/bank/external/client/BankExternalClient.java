package com.woorifisan.platform.domain.bank.external.client;

import com.woorifisan.platform.domain.bank.dto.response.BalanceInquiryResponse;
import com.woorifisan.platform.domain.bank.dto.response.HistoryInquiryResponse;
import com.woorifisan.platform.domain.bank.dto.response.TransferResponse;
import com.woorifisan.platform.domain.bank.external.dto.BankBalanceInquiryRequest;
import com.woorifisan.platform.domain.bank.external.dto.BankHistoryInquiryRequest;
import com.woorifisan.platform.domain.bank.external.dto.BankWithdrawalRequest;
import com.woorifisan.platform.global.config.BankNetworkConfig;
import com.woorifisan.platform.global.config.BankNetworkConfig.BankProperty;
import com.woorifisan.platform.global.exception.BankCoreException;
import com.woorifisan.platform.global.exception.BusinessException;
import com.woorifisan.platform.global.response.ApiResponse;
import com.woorifisan.platform.global.response.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
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

        try {
            ApiResponse<BalanceInquiryResponse> response = webClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .retrieve()
                    // 4xx, 5xx 에러 발생 시 바디를 읽어서 BankCoreException으로 변환
                    .onStatus(HttpStatusCode::isError, clientResponse ->
                        clientResponse.bodyToMono(new ParameterizedTypeReference<ApiResponse<BalanceInquiryResponse>>() {})
                            .flatMap(errorBody -> {
                                String bankErrorCode    = (errorBody.getError() != null) ? errorBody.getError().getCode()    : "UNKNOWN";
                                String bankErrorMessage = (errorBody.getError() != null) ? errorBody.getError().getMessage() : "UNKNOWN";
                                return Mono.error(new BankCoreException(mapToInternalErrorCode(bankErrorCode), bankErrorCode, bankErrorMessage));
                            })
                    )
                    .bodyToMono(new ParameterizedTypeReference<ApiResponse<BalanceInquiryResponse>>() {})
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
     * 특정 은행의 출금 API를 호출합니다.
     *
     * @param bankCode 은행 코드
     * @param request  은행 전용 출금 요청 DTO
     * @return 출금 결과 응답
     */
    public TransferResponse withdraw(String bankCode, BankWithdrawalRequest request) {
        BankProperty bankProperty = bankNetworkConfig.getBankProperty(bankCode);

        if (bankProperty == null) {
            log.error("지원하지 않는 은행 코드입니다: {}", bankCode);
            throw new BusinessException(ErrorCode.BANK_NOT_FOUND);
        }

        String url = bankProperty.getUrl("withdraw");

        try {
            ApiResponse<TransferResponse> response = webClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, clientResponse ->
                        clientResponse.bodyToMono(new ParameterizedTypeReference<ApiResponse<TransferResponse>>() {})
                            .flatMap(errorBody -> {
                                String bankErrorCode    = (errorBody.getError() != null) ? errorBody.getError().getCode()    : "UNKNOWN";
                                String bankErrorMessage = (errorBody.getError() != null) ? errorBody.getError().getMessage() : "UNKNOWN";
                                return Mono.error(new BankCoreException(mapToInternalErrorCode(bankErrorCode), bankErrorCode, bankErrorMessage));
                            })
                    )
                    .bodyToMono(new ParameterizedTypeReference<ApiResponse<TransferResponse>>() {})
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
     * 특정 은행의 거래 내역 조회 API를 호출합니다.
     *
     * @param bankCode 은행 코드
     * @param request  은행 전용 거래 내역 조회 요청 DTO
     * @return 거래 내역 조회 결과 응답
     */
    public HistoryInquiryResponse fetchHistory(String bankCode, BankHistoryInquiryRequest request) {
        BankProperty bankProperty = bankNetworkConfig.getBankProperty(bankCode);

        if (bankProperty == null) {
            log.error("지원하지 않는 은행 코드입니다: {}", bankCode);
            throw new BusinessException(ErrorCode.BANK_NOT_FOUND);
        }

        String url = bankProperty.getUrl("history");

        try {
            ApiResponse<HistoryInquiryResponse> response = webClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, clientResponse ->
                            clientResponse.bodyToMono(new ParameterizedTypeReference<ApiResponse<HistoryInquiryResponse>>() {})
                                    .flatMap(errorBody -> {
                                        String bankErrorCode    = (errorBody.getError() != null) ? errorBody.getError().getCode()    : "UNKNOWN";
                                        String bankErrorMessage = (errorBody.getError() != null) ? errorBody.getError().getMessage() : "UNKNOWN";
                                        return Mono.error(new BankCoreException(mapToInternalErrorCode(bankErrorCode), bankErrorCode, bankErrorMessage));
                                    })
                    )
                    .bodyToMono(new ParameterizedTypeReference<ApiResponse<HistoryInquiryResponse>>() {})
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
     * 은행 코어의 에러 코드를 플랫폼 내부 에러 코드로 변환합니다.
     */
    private ErrorCode mapToInternalErrorCode(String bankErrorCode) {
        return switch (bankErrorCode) {
            case "ACC_001" -> ErrorCode.INQUIRY_ACCOUNT_NOTFOUND;
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

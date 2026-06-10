package com.woorifisan.platform.domain.bank.external.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.woorifisan.platform.domain.bank.external.dto.BankLoanEvaluateRequest;
import com.woorifisan.platform.domain.bank.external.dto.BankLoanExecuteRequest;
import com.woorifisan.platform.global.exception.BusinessException;
import com.woorifisan.platform.global.response.ApiResponse;
import com.woorifisan.platform.global.response.ErrorCode;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import com.woorifisan.platform.global.config.BankNetworkConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

// Bank 코어 대출 API와의 HTTP 통신을 전담하는 클라이언트
// BaaS(계좌/이체) API는 BankExternalClient, 대출 API는 이 클래스가 담당
@Slf4j
@Component
public class BankLoanClient {

    private final WebClient bankWebClient;
    private final BankNetworkConfig bankNetworkConfig;
    private final ObjectMapper objectMapper;

    private static final String DEFAULT_BANK_CODE = "020";

    public BankLoanClient(WebClient webClient,
                          ObjectMapper objectMapper,
                          BankNetworkConfig bankNetworkConfig) {
        this.bankWebClient = webClient;
        this.bankNetworkConfig = bankNetworkConfig;
        this.objectMapper  = objectMapper;
    }

    // GET /api/v1/loan/evaluation/terms
    public List<Map<String, Object>> getEvaluationTerms(String bankCode) {
        BankNetworkConfig.BankProperty bankProperty = bankNetworkConfig.getBankProperty(bankCode);
        if (bankProperty == null) {
            throw new BusinessException(ErrorCode.BANK_NOT_FOUND);
        }
        String uri = bankProperty.getUrl("evaluation-terms");
        return getRequest(uri,
                new ParameterizedTypeReference<ApiResponse<List<Map<String, Object>>>>() {});
    }

    // POST /api/v1/loan/evaluation (multipart: data 파트 JSON + files 파트)
    public Map<String, Object> submitEvaluation(String bankCode, BankLoanEvaluateRequest data, List<MultipartFile> files) {
        BankNetworkConfig.BankProperty bankProperty = bankNetworkConfig.getBankProperty(bankCode);
        if (bankProperty == null) {
            throw new BusinessException(ErrorCode.BANK_NOT_FOUND);
        }
        String uri = bankProperty.getUrl("evaluation");
        MultipartBodyBuilder builder = buildMultipart(data, files);
        return postMultipartRequest(uri, builder,
                new ParameterizedTypeReference<ApiResponse<Map<String, Object>>>() {},
                Duration.ofSeconds(10));
    }

    // GET /api/v1/loan/contract/terms/{productId}/{loanNo}
    public List<Map<String, Object>> getContractTerms(String bankCode, String productId, String loanNo) {
        BankNetworkConfig.BankProperty bankProperty = bankNetworkConfig.getBankProperty(bankCode);
        if (bankProperty == null) {
            throw new BusinessException(ErrorCode.BANK_NOT_FOUND);
        }
        String uriPrefix = bankProperty.getUrl("contract-terms");
        String uri = uriPrefix + "/" + productId + "/" + loanNo;
        return getRequest(uri,
                new ParameterizedTypeReference<ApiResponse<List<Map<String, Object>>>>() {});
    }

    // POST /api/v1/loan/execution
    public Map<String, Object> executeLoan(String bankCode, BankLoanExecuteRequest request) {
        BankNetworkConfig.BankProperty bankProperty = bankNetworkConfig.getBankProperty(bankCode);
        if (bankProperty == null) {
            throw new BusinessException(ErrorCode.BANK_NOT_FOUND);
        }
        String uri = bankProperty.getUrl("execution");
        return postRequest(uri, request,
                new ParameterizedTypeReference<ApiResponse<Map<String, Object>>>() {},
                Duration.ofSeconds(5));
    }

    /* Helpers */
    private <T> T getRequest(String uri, ParameterizedTypeReference<ApiResponse<T>> type) {
        try {
            ApiResponse<T> response = bankWebClient.get()
                    .uri(uri)
                    .retrieve()
                    .bodyToMono(type)
                    .block(Duration.ofSeconds(5));
            return unwrap(response, uri);
        } catch (BusinessException e) {
            throw e;
        } catch (WebClientResponseException e) {
            log.warn("[BankLoanClient] GET 오류 - uri: {}, status: {}", uri, e.getStatusCode());
            throw new BusinessException(ErrorCode.LOAN_BANK_ROUTING_ERROR, extractErrorMessage(e));
        } catch (Exception e) {
            log.error("[BankLoanClient] GET 실패 - uri: {}", uri, e);
            throw new BusinessException(ErrorCode.LOAN_BANK_ROUTING_ERROR);
        }
    }

    private <T, R> T postRequest(String uri, R body,
                                  ParameterizedTypeReference<ApiResponse<T>> type, Duration timeout) {
        try {
            ApiResponse<T> response = bankWebClient.post()
                    .uri(uri)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(type)
                    .block(timeout);
            return unwrap(response, uri);
        } catch (BusinessException e) {
            throw e;
        } catch (WebClientResponseException e) {
            log.warn("[BankLoanClient] POST 오류 - uri: {}, status: {}", uri, e.getStatusCode());
            throw new BusinessException(ErrorCode.LOAN_BANK_ROUTING_ERROR, extractErrorMessage(e));
        } catch (Exception e) {
            log.error("[BankLoanClient] POST 실패 - uri: {}", uri, e);
            throw new BusinessException(ErrorCode.LOAN_BANK_ROUTING_ERROR);
        }
    }

    private <T> T postMultipartRequest(String uri, MultipartBodyBuilder builder,
                                        ParameterizedTypeReference<ApiResponse<T>> type, Duration timeout) {
        try {
            ApiResponse<T> response = bankWebClient.post()
                    .uri(uri)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(BodyInserters.fromMultipartData(builder.build()))
                    .retrieve()
                    .bodyToMono(type)
                    .block(timeout);
            return unwrap(response, uri);
        } catch (BusinessException e) {
            throw e;
        } catch (WebClientResponseException e) {
            log.warn("[BankLoanClient] POST(multipart) 오류 - uri: {}, status: {}", uri, e.getStatusCode());
            throw new BusinessException(ErrorCode.LOAN_BANK_ROUTING_ERROR, extractErrorMessage(e));
        } catch (Exception e) {
            log.error("[BankLoanClient] POST(multipart) 실패 - uri: {}", uri, e);
            throw new BusinessException(ErrorCode.LOAN_BANK_ROUTING_ERROR);
        }
    }

    private MultipartBodyBuilder buildMultipart(BankLoanEvaluateRequest data, List<MultipartFile> files) {
        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        try {
            builder.part("data", objectMapper.writeValueAsString(data))
                   .contentType(MediaType.APPLICATION_JSON);
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCode.LOAN_FILE_FORWARD_ERROR);
        }
        for (MultipartFile file : files) {
            try {
                final byte[] bytes    = file.getBytes();
                final String filename = file.getOriginalFilename();
                builder.part("files", new ByteArrayResource(bytes) {
                    @Override public String getFilename() { return filename; }
                }).contentType(MediaType.parseMediaType(
                        file.getContentType() != null ? file.getContentType() : "application/octet-stream"));
            } catch (IOException e) {
                log.error("[BankLoanClient] 파일 읽기 실패 - {}", file.getOriginalFilename(), e);
                throw new BusinessException(ErrorCode.LOAN_FILE_FORWARD_ERROR);
            }
        }
        return builder;
    }

    private <T> T unwrap(ApiResponse<T> response, String uri) {
        if (response == null || response.getData() == null) {
            log.warn("[BankLoanClient] 빈 응답 - uri: {}", uri);
            throw new BusinessException(ErrorCode.BANK_API_ERROR);
        }
        return response.getData();
    }

    private String extractErrorMessage(WebClientResponseException e) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> body = objectMapper.readValue(e.getResponseBodyAsString(), Map.class);
            @SuppressWarnings("unchecked")
            Map<String, Object> err = (Map<String, Object>) body.get("error");
            if (err != null && err.get("message") != null) return String.valueOf(err.get("message"));
        } catch (Exception ignored) {}
        return null;
    }
}

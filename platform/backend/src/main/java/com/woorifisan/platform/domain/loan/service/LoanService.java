package com.woorifisan.platform.domain.loan.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.woorifisan.platform.domain.bank.mapper.BankMapper;
import com.woorifisan.platform.domain.bank.model.Bank;
import com.woorifisan.platform.domain.loan.dto.request.LoanEvaluateRequest;
import com.woorifisan.platform.domain.loan.dto.request.LoanExecuteRequest;
import com.woorifisan.platform.domain.loan.dto.response.AvailableProductDto;
import com.woorifisan.platform.domain.loan.dto.response.LoanContractDocumentsResponse;
import com.woorifisan.platform.domain.loan.dto.response.LoanEvaluateResponse;
import com.woorifisan.platform.domain.loan.dto.response.LoanEvaluationResultResponse;
import com.woorifisan.platform.domain.loan.dto.response.LoanExecuteResponse;
import com.woorifisan.platform.domain.loan.dto.response.LoanRequiredDocumentsResponse;
import com.woorifisan.platform.domain.loan.dto.response.TermsDocumentDto;
import com.woorifisan.platform.global.exception.BusinessException;
import com.woorifisan.platform.global.response.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class LoanService {

    private static final String LOAN_GUID_PREFIX = "LN-";
    private static final String REDIS_EVAL_GUID_KEY = "loan:eval:%s:guid";
    private static final String REDIS_APP_GUID_KEY = "loan:app:%s:guid";
    private static final String REDIS_APP_EVAL_KEY = "loan:app:%s:evalId";
    private static final String REDIS_APP_RESULT_KEY = "loan:app:%s:result"; // 실제 심사 결과 캐싱용
    private static final String REDIS_LOAN_NO_ID_MAP = "loan:loanNo:%s:id"; // loanNo -> evaluationId 매핑용
    private static final long GUID_TTL_HOURS = 24L;
    private static final long SSE_MOCK_DELAY_MS = 2_000L;

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final Executor sseTaskExecutor;
    private final WebClient bankWebClient;
    private final BankMapper bankMapper;

    public LoanService(StringRedisTemplate redisTemplate,
                       ObjectMapper objectMapper,
                       @Qualifier("sseTaskExecutor") Executor sseTaskExecutor,
                       WebClient bankWebClient,
                       BankMapper bankMapper,
                       @Value("${bank.core.url}") String bankCoreUrl) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.sseTaskExecutor = sseTaskExecutor;
        this.bankMapper = bankMapper;
        this.bankWebClient = bankWebClient.mutate()
                .baseUrl(bankCoreUrl)
                .build();
    }

    /**
     * Step 1 — 심사 서류 조회 (BK-B11 연동)
     */
    public LoanRequiredDocumentsResponse getRequiredDocuments(Long staffId) {
        String guid = generateGuid();
        log.info("[{}] 심사 서류 조회 요청 - staffId: {}", guid, staffId);

        try {
            // 은행 코어의 /api/v1/loan/evaluation/terms 호출
            com.woorifisan.platform.global.response.ApiResponse<List<Map<String, Object>>> bankResponse = bankWebClient.get()
                    .uri("/api/v1/loan/evaluation/terms")
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<com.woorifisan.platform.global.response.ApiResponse<List<Map<String, Object>>>>() {})
                    .block();

            if (bankResponse == null || bankResponse.getData() == null) {
                throw new BusinessException(ErrorCode.BANK_API_ERROR);
            }

            // 은행 응답을 플랫폼 DTO로 변환
            List<TermsDocumentDto> documents = bankResponse.getData().stream()
                    .map(terms -> TermsDocumentDto.builder()
                            .documentType(String.valueOf(terms.get("termsCode")))
                            .documentName(String.valueOf(terms.get("title")))
                            .documentUrl(String.valueOf(terms.get("termsUrl")))
                            .documentContent(terms.get("termsContent") != null ? String.valueOf(terms.get("termsContent")) : null)
                            .isMandatory(Boolean.TRUE.equals(terms.get("isMandatory")))
                            .build())
                    .toList();

            log.info("[{}] 심사 서류 조회 완료 - count: {}", guid, documents.size());
            return LoanRequiredDocumentsResponse.builder().documents(documents).build();
        } catch (Exception e) {
            log.error("[{}] 은행 API 연동 중 오류 발생 (심사 서류)", guid, e);
            throw new BusinessException(ErrorCode.LOAN_BANK_ROUTING_ERROR);
        }
    }

    /**
     * Step 2 — 서류 제출 및 심사 요청 (BK-B12~B19 연동)
     */
    public LoanEvaluateResponse evaluateLoan(LoanEvaluateRequest request, Long staffId) {
        // 플랫폼 bank 테이블(DB)에서 은행 코드 검증
        Bank bank = bankMapper.findByBankCode(request.getBankCode())
                .filter(Bank::isActive)
                .orElseThrow(() -> new BusinessException(ErrorCode.BANK_NOT_FOUND));

        if (!bank.getBankCode().equals(request.getDepositBankCode())) {
            throw new BusinessException(ErrorCode.LOAN_DEPOSIT_BANK_MISMATCH);
        }

        String guid = generateGuid();
        String applicationId = generateApplicationId();
        LocalDateTime receivedAt = LocalDateTime.now();

        log.info("[{}] 대출 심사 요청 시작 - staffId: {}, bankCode: {}, customerName: {}, applicationId: {}",
                guid, staffId, request.getBankCode(), maskName(request.getCustomerName()), applicationId);

        // 3. 은행 API 규격에 맞게 데이터 변환 (Mapping)
        Map<String, Object> bankRequest = new HashMap<>();
        bankRequest.put("customerName", request.getCustomerName());
        bankRequest.put("customerRrnPrefix", request.getCustomerRrnPrefix());
        bankRequest.put("depositBankCode", request.getDepositBankCode());
        bankRequest.put("depositAccountNo", request.getDepositAccountNo());
        // 현재 화면에서 금액 입력을 안 받으므로 한도 조회를 위해 임의의 큰 금액 전달
        bankRequest.put("requestedAmount", new BigDecimal("100000000"));
        bankRequest.put("requestedPeriod", 60);
        bankRequest.put("isCreditInfoAgreed", hasAgreed(request, "CREDIT_INFO_AGREE"));
        bankRequest.put("isProductTermsAgreed", hasAgreed(request, "NICE_CREDIT_INQUIRY"));
        bankRequest.put("isDocumentCollected", hasAgreed(request, "DOCUMENT_COLLECT"));

        try {
            com.woorifisan.platform.global.response.ApiResponse<Map<String, Object>> bankResponse = bankWebClient.post()
                    .uri("/api/v1/loan/evaluation")
                    .bodyValue(bankRequest)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<com.woorifisan.platform.global.response.ApiResponse<Map<String, Object>>>() {})
                    .block();

            if (bankResponse == null || bankResponse.getData() == null) {
                throw new BusinessException(ErrorCode.BANK_API_ERROR);
            }

            Map<String, Object> evalData = bankResponse.getData();
            String evaluationId = String.valueOf(evalData.get("evaluationId")); // 은행 내부 ID (Long)
            String loanNo = String.valueOf(evalData.get("loanNo"));           // 플랫폼용 식별자 (LN-XXX)

            // Redis 상태 저장 (SSE 및 다음 단계에서 사용)
            redisTemplate.opsForValue().set(String.format(REDIS_APP_GUID_KEY, applicationId), guid, GUID_TTL_HOURS, TimeUnit.HOURS);
            redisTemplate.opsForValue().set(String.format(REDIS_APP_EVAL_KEY, applicationId), loanNo, GUID_TTL_HOURS, TimeUnit.HOURS);
            redisTemplate.opsForValue().set(String.format(REDIS_EVAL_GUID_KEY, loanNo), guid, GUID_TTL_HOURS, TimeUnit.HOURS);
            redisTemplate.opsForValue().set(String.format(REDIS_LOAN_NO_ID_MAP, loanNo), evaluationId, GUID_TTL_HOURS, TimeUnit.HOURS);

            // 4. 심사 결과를 Redis에 캐싱하여 SSE에서 즉시 응답하도록 함
            redisTemplate.opsForValue().set(String.format(REDIS_APP_RESULT_KEY, applicationId),
                    objectMapper.writeValueAsString(evalData), GUID_TTL_HOURS, TimeUnit.HOURS);

            log.info("[{}] 대출 심사 접수 및 결과 캐싱 완료 - loanNo: {}", guid, loanNo);
            return LoanEvaluateResponse.builder()
                    .applicationId(applicationId)
                    .receivedAt(receivedAt.toString())
                    .build();

        } catch (org.springframework.web.reactive.function.client.WebClientResponseException e) {
            log.warn("[{}] 은행 API 오류 응답 (심사 신청) - status: {}, body: {}", guid, e.getStatusCode(), e.getResponseBodyAsString());
            String bankMsg = extractBankErrorMessage(e);
            throw new BusinessException(ErrorCode.LOAN_BANK_ROUTING_ERROR, bankMsg);
        } catch (Exception e) {
            log.error("[{}] 은행 API 연동 중 오류 발생 (심사 신청)", guid, e);
            throw new BusinessException(ErrorCode.LOAN_BANK_ROUTING_ERROR);
        }
    }

    private boolean hasAgreed(LoanEvaluateRequest request, String type) {
        if (request.getDocuments() == null) return false;
        return request.getDocuments().stream().anyMatch(d -> type.equals(d.getDocumentType()));
    }

    /**
     * Step 3 — 심사 결과 스트리밍 (SSE)
     */
    public SseEmitter streamEvaluationResult(String applicationId, Long staffId) {
        String guid = resolveGuidForApp(applicationId);
        String loanNo = resolveEvaluationIdForApp(applicationId); // 사용하지 않던 로직 활용
        log.info("[{}] SSE 심사 결과 스트림 시작 - staffId: {}, loanNo: {}, applicationId: {}", guid, staffId, loanNo, applicationId);

        SseEmitter emitter = new SseEmitter(30_000L);
        emitter.onTimeout(emitter::complete);
        emitter.onError(e -> log.warn("[{}] SSE 연결 오류", guid));

        CompletableFuture.runAsync(() -> {
            try {
                // 1) PENDING 상태 먼저 전송
                sendSse(emitter, guid, "status", LoanEvaluationResultResponse.builder()
                        .applicationId(applicationId)
                        .evaluationStatus("PENDING")
                        .requestedAt(LocalDateTime.now().toString()).build());

                Thread.sleep(SSE_MOCK_DELAY_MS); // UI 체감을 위한 약간의 지연

                // 2) Redis에 저장된 실제 심사 결과 꺼내기
                String resultJson = redisTemplate.opsForValue().get(String.format(REDIS_APP_RESULT_KEY, applicationId));
                if (resultJson == null) throw new BusinessException(ErrorCode.LOAN_EVALUATION_NOT_FOUND);

                Map<String, Object> evalData = objectMapper.readValue(resultJson, new TypeReference<>() {});
                String status = String.valueOf(evalData.get("status"));
                
                LoanEvaluationResultResponse result;
                if ("APPROVED".equals(status)) {
                    // 은행 코어의 AvailableProductDto 필드명(productId, productName, minLimit, maxLimit, minRate)에 맞춰 매핑 수정
                    List<Map<String, Object>> bankProducts = (List<Map<String, Object>>) evalData.get("availableProducts");
                    List<AvailableProductDto> products = bankProducts.stream()
                            .map(p -> {
                                Object minLimit = p.get("minLimit");
                                Object maxLimit = p.get("maxLimit");
                                Object minRate = p.get("minRate");
                                return AvailableProductDto.builder()
                                        .loanProductCode(String.valueOf(p.get("productId")))
                                        .loanProductName(String.valueOf(p.get("productName")))
                                        .minAmount(minLimit != null ? new BigDecimal(minLimit.toString()) : BigDecimal.ZERO)
                                        .maxAmount(maxLimit != null ? new BigDecimal(maxLimit.toString()) : BigDecimal.ZERO)
                                        .interestRate(minRate != null ? new BigDecimal(minRate.toString()) : BigDecimal.ZERO)
                                        .loanPeriodMonths(36).build();
                            }).toList();

                    result = LoanEvaluationResultResponse.builder()
                            .applicationId(applicationId)
                            .evaluationStatus("APPROVED")
                            .evaluationId(String.valueOf(evalData.get("loanNo")))
                            .approvedLimit(new BigDecimal(String.valueOf(evalData.get("approvedLimit"))))
                            .availableProducts(products).build();
                } else {
                    result = LoanEvaluationResultResponse.builder()
                            .applicationId(applicationId)
                            .evaluationStatus("REJECTED")
                            .rejectionMessage(String.valueOf(evalData.get("rejectReason"))).build();
                }

                sendSse(emitter, guid, "status", result);
                log.info("[{}] SSE 심사 결과 전송 완료 - loanNo: {}, status: {}", guid, loanNo, status);
                emitter.complete();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("[{}] SSE 스레드 중단", guid);
            } catch (Exception e) {
                log.error("[{}] SSE 처리 중 예외 발생", guid, e);
                emitter.completeWithError(e);
            }
        }, sseTaskExecutor);

        return emitter;
    }

    private void sendSse(SseEmitter emitter, String guid, String name, Object data) {
        try {
            emitter.send(SseEmitter.event().name(name).data(objectMapper.writeValueAsString(data)));
        } catch (AsyncRequestNotUsableException e) {
            log.warn("[{}] SSE 이미 종료된 연결에 대한 전송 시도 (AsyncRequestNotUsableException)", guid);
        } catch (java.io.IOException e) {
            log.warn("[{}] SSE 클라이언트 연결 중단 (IOException): {}", guid, e.getMessage());
        } catch (Exception e) {
            log.error("[{}] SSE 전송 중 예외 발생", guid, e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Step 5 — 계약 서류 조회 (BK-B20 연동)
     */
    public LoanContractDocumentsResponse getContractDocuments(String loanProductCode, String evaluationId, Long staffId) {
        String guid = resolveGuidForEval(evaluationId);
        log.info("[{}] 계약 서류 조회 시작 - staffId: {}, loanNo: {}, productId: {}", guid, staffId, evaluationId, loanProductCode);

        // Redis에서 은행 내부 ID(Long) 조회
        String bankInternalId = redisTemplate.opsForValue().get(String.format(REDIS_LOAN_NO_ID_MAP, evaluationId));
        if (bankInternalId == null) throw new BusinessException(ErrorCode.LOAN_EVALUATION_NOT_FOUND);

        try {
            com.woorifisan.platform.global.response.ApiResponse<List<Map<String, Object>>> bankResponse = bankWebClient.get()
                    .uri("/api/v1/loan/contract/terms/{productId}/{evaluationId}", loanProductCode, bankInternalId)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<com.woorifisan.platform.global.response.ApiResponse<List<Map<String, Object>>>>() {})
                    .block();

            if (bankResponse == null || bankResponse.getData() == null) throw new BusinessException(ErrorCode.BANK_API_ERROR);

            List<TermsDocumentDto> documents = bankResponse.getData().stream()
                    .map(terms -> TermsDocumentDto.builder()
                            .documentType(String.valueOf(terms.get("termsCode")))
                            .documentName(String.valueOf(terms.get("title")))
                            .documentUrl(String.valueOf(terms.get("termsUrl")))
                            .documentContent(terms.get("termsContent") != null ? String.valueOf(terms.get("termsContent")) : null)
                            .isMandatory(Boolean.TRUE.equals(terms.get("isMandatory"))).build()).toList();

            log.info("[{}] 계약 서류 조회 완료 - count: {}", guid, documents.size());
            return LoanContractDocumentsResponse.builder()
                    .loanProductCode(loanProductCode)
                    .loanProductName("심사 승인 상품")
                    .documents(documents).build();
        } catch (Exception e) {
            log.error("[{}] 은행 API 연동 중 오류 발생 (계약 서류)", guid, e);
            throw new BusinessException(ErrorCode.LOAN_BANK_ROUTING_ERROR);
        }
    }

    /**
     * Step 6 — 대출 실행 (BK-B21~B23 연동)
     */
    public LoanExecuteResponse executeLoan(LoanExecuteRequest request, Long staffId) {
        String guid = resolveGuidForEval(request.getEvaluationId());
        log.info("[{}] 대출 실행 요청 시작 - staffId: {}, loanNo: {}, executeAmount: {}", 
                guid, staffId, request.getEvaluationId(), request.getExecuteAmount());

        long productId;
        try {
            productId = Long.parseLong(request.getLoanProductCode());
        } catch (NumberFormatException e) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }

        Map<String, Object> bankRequest = new HashMap<>();
        bankRequest.put("loanNo", request.getEvaluationId());
        bankRequest.put("productId", productId);
        bankRequest.put("loanAmount", request.getExecuteAmount());
        bankRequest.put("repaymentPeriod", request.getRepaymentPeriod());
        bankRequest.put("repaymentType", "원리금균등");
        bankRequest.put("accountPassword", request.getAccountPassword());

        try {
            com.woorifisan.platform.global.response.ApiResponse<Map<String, Object>> bankResponse = bankWebClient.post()
                    .uri("/api/v1/loan/execution")
                    .bodyValue(bankRequest)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<com.woorifisan.platform.global.response.ApiResponse<Map<String, Object>>>() {})
                    .block();

            if (bankResponse == null || bankResponse.getData() == null) throw new BusinessException(ErrorCode.BANK_API_ERROR);

            Map<String, Object> execData = bankResponse.getData();
            log.info("[{}] 대출 실행 완료 - loanNo: {}", guid, execData.get("loanNo"));

            return LoanExecuteResponse.builder()
                    .loanId(String.valueOf(execData.get("loanNo")))
                    .borrowerName(String.valueOf(execData.get("customerName")))
                    .depositTransactionId("TXN-" + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase())
                    .loanBalance(new BigDecimal(String.valueOf(execData.get("loanAmount"))))
                    .executeAmount(new BigDecimal(String.valueOf(execData.get("loanAmount"))))
                    .interestRate(new BigDecimal(String.valueOf(execData.get("interestRate"))))
                    .repaymentPeriod(Integer.parseInt(String.valueOf(execData.get("repaymentPeriod"))))
                    .monthlyPayment(new BigDecimal(String.valueOf(execData.get("monthlyPayment"))))
                    .repaymentStartDate(String.valueOf(execData.get("startDate")))
                    .maturityDate(String.valueOf(execData.get("endDate"))).build();

        } catch (org.springframework.web.reactive.function.client.WebClientResponseException e) {
            log.warn("[{}] 은행 API 오류 응답 (대출 실행) - status: {}, body: {}", guid, e.getStatusCode(), e.getResponseBodyAsString());
            String bankMsg = extractBankErrorMessage(e);
            throw new BusinessException(ErrorCode.LOAN_BANK_ROUTING_ERROR, bankMsg);
        } catch (Exception e) {
            log.error("[{}] 은행 API 연동 중 오류 발생 (대출 실행)", guid, e);
            throw new BusinessException(ErrorCode.LOAN_BANK_ROUTING_ERROR);
        }
    }

    // --- Private helpers ---
    private String extractBankErrorMessage(org.springframework.web.reactive.function.client.WebClientResponseException e) {
        try {
            Map<String, Object> body = objectMapper.readValue(e.getResponseBodyAsString(), new TypeReference<>() {});
            @SuppressWarnings("unchecked")
            Map<String, Object> err = (Map<String, Object>) body.get("error");
            if (err != null && err.get("message") != null) {
                return String.valueOf(err.get("message"));
            }
        } catch (Exception ignored) {}
        return null;
    }

    private String generateGuid() { return LOAN_GUID_PREFIX + UUID.randomUUID().toString().toUpperCase(); }
    private String generateApplicationId() { return "APP-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase(); }
    
    private String resolveGuidForApp(String applicationId) {
        String guid = redisTemplate.opsForValue().get(String.format(REDIS_APP_GUID_KEY, applicationId));
        if (guid == null) throw new BusinessException(ErrorCode.LOAN_EVALUATION_NOT_FOUND);
        return guid;
    }

    private String resolveEvaluationIdForApp(String applicationId) {
        String evaluationId = redisTemplate.opsForValue().get(String.format(REDIS_APP_EVAL_KEY, applicationId));
        if (evaluationId == null) throw new BusinessException(ErrorCode.LOAN_EVALUATION_NOT_FOUND);
        return evaluationId;
    }

    private String resolveGuidForEval(String evaluationId) {
        String guid = redisTemplate.opsForValue().get(String.format(REDIS_EVAL_GUID_KEY, evaluationId));
        if (guid == null) throw new BusinessException(ErrorCode.LOAN_EVALUATION_NOT_FOUND);
        return guid;
    }

    private String maskName(String name) {
        if (name == null || name.length() < 2) return "**";
        return name.charAt(0) + "*".repeat(name.length() - 1);
    }
}

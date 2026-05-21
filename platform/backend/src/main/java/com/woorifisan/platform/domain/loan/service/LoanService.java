package com.woorifisan.platform.domain.loan.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.woorifisan.platform.domain.loan.dto.response.AvailableProductDto;
import com.woorifisan.platform.domain.loan.dto.response.LoanContractDocumentsResponse;
import com.woorifisan.platform.domain.loan.dto.request.LoanEvaluateRequest;
import com.woorifisan.platform.domain.loan.dto.response.LoanEvaluateResponse;
import com.woorifisan.platform.domain.loan.dto.response.LoanEvaluationResultResponse;
import com.woorifisan.platform.domain.loan.dto.request.LoanExecuteRequest;
import com.woorifisan.platform.domain.loan.dto.response.LoanExecuteResponse;
import com.woorifisan.platform.domain.loan.dto.response.LoanRequiredDocumentsResponse;
import com.woorifisan.platform.domain.loan.dto.response.TermsDocumentDto;
import com.woorifisan.platform.global.exception.BusinessException;
import com.woorifisan.platform.global.response.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
    private static final long GUID_TTL_HOURS = 24L;
    private static final long SSE_MOCK_DELAY_MS = 3_000L;

    private static final Map<String, String> PRODUCT_NAME_MAP = Map.of(
            "LP001", "우리 직장인 신용대출",
            "LP002", "우리 든든 직장인 대출",
            "LP003", "우리 스마트론",
            "LP004", "우리 프리미엄 직장인 대출",
            "LP005", "우리 직장인 플러스론"
    );

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final Executor sseTaskExecutor;

    public LoanService(StringRedisTemplate redisTemplate,
                       ObjectMapper objectMapper,
                       @Qualifier("sseTaskExecutor") Executor sseTaskExecutor) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.sseTaskExecutor = sseTaskExecutor;
    }

    /**
     * Step 1 — 심사 서류 조회 (BK-B11)
     */
    public LoanRequiredDocumentsResponse getRequiredDocuments(Long staffId) {
        String guid = generateGuid();
        log.info("[{}] 심사 서류 조회 요청 - staffId: {}", guid, staffId);
        LoanRequiredDocumentsResponse response = buildMockRequiredDocuments();
        log.info("[{}] 심사 서류 조회 완료", guid);
        return response;
    }

    /**
     * Step 2 — 서류 제출 및 심사 요청 (BK-B12~B19)
     * applicationId를 생성하고 Redis에 evaluationId와 GUID를 저장한다.
     */
    public LoanEvaluateResponse evaluateLoan(LoanEvaluateRequest request, Long staffId) {
        String guid = generateGuid();
        String applicationId = generateApplicationId();
        String evaluationId = generateEvaluationId();
        LocalDateTime receivedAt = LocalDateTime.now();

        log.info("[{}] 대출 심사 요청 - staffId: {}, bankCode: {}, customerName: {}, applicationId: {}",
                guid, staffId, request.getBankCode(), maskName(request.getCustomerName()), applicationId);

        // applicationId → guid 저장 (Step 3 로깅용)
        redisTemplate.opsForValue().set(
                String.format(REDIS_APP_GUID_KEY, applicationId), guid, GUID_TTL_HOURS, TimeUnit.HOURS);
        // applicationId → evaluationId 저장 (Step 3에서 evaluationId 반환용)
        redisTemplate.opsForValue().set(
                String.format(REDIS_APP_EVAL_KEY, applicationId), evaluationId, GUID_TTL_HOURS, TimeUnit.HOURS);
        // evaluationId → guid 저장 (Step 5, 6 로깅용)
        redisTemplate.opsForValue().set(
                String.format(REDIS_EVAL_GUID_KEY, evaluationId), guid, GUID_TTL_HOURS, TimeUnit.HOURS);

        log.info("[{}] 대출 심사 접수 완료 - applicationId: {}, evaluationId: {}", guid, applicationId, evaluationId);
        return LoanEvaluateResponse.builder()
                .applicationId(applicationId)
                .receivedAt(receivedAt.toString())
                .build();
    }

    /**
     * Step 3 — 심사 결과 스트리밍 (SSE) (BK-B19)
     * 연결 즉시 PENDING 이벤트를 전송하고, SSE_MOCK_DELAY_MS 후 최종 결과를 push한다.
     */
    public SseEmitter streamEvaluationResult(String applicationId, Long staffId) {
        String guid = resolveGuidForApp(applicationId);
        String evaluationId = resolveEvaluationIdForApp(applicationId);

        log.info("[{}] SSE 심사 결과 스트림 시작 - staffId: {}, applicationId: {}", guid, staffId, applicationId);

        SseEmitter emitter = new SseEmitter(30_000L);

        // 타임아웃(30s) 도달 시 연결을 정상 종료해 HTTP 커넥션을 해제한다.
        // 등록하지 않으면 Spring이 내부적으로 completeWithError를 호출해 클라이언트에 오류로 전달된다.
        emitter.onTimeout(() -> {
            log.warn("[{}] SSE 타임아웃 - applicationId: {}", guid, applicationId);
            emitter.complete();
        });

        // 클라이언트가 연결을 끊었을 때(브라우저 닫기, 네트워크 단절 등) 로그만 남긴다.
        emitter.onError(e -> log.warn("[{}] SSE 연결 오류 - applicationId: {}", guid, applicationId));

        // ForkJoinPool.commonPool() 대신 SSE 전용 스레드 풀을 사용해 공용 풀 점유를 방지한다.
        CompletableFuture.runAsync(() -> {
            try {
                LoanEvaluationResultResponse pending = LoanEvaluationResultResponse.builder()
                        .applicationId(applicationId)
                        .evaluationStatus("PENDING")
                        .requestedAt(LocalDateTime.now().toString())
                        .build();
                emitter.send(SseEmitter.event()
                        .name("status")
                        .data(objectMapper.writeValueAsString(pending)));

                Thread.sleep(SSE_MOCK_DELAY_MS);

                LoanEvaluationResultResponse result = buildMockEvaluationResult(applicationId, evaluationId);
                emitter.send(SseEmitter.event()
                        .name("status")
                        .data(objectMapper.writeValueAsString(result)));

                log.info("[{}] SSE 심사 결과 전송 완료 - status: {}", guid, result.getEvaluationStatus());
                emitter.complete();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                emitter.completeWithError(e);
            } catch (Exception e) {
                log.error("[{}] SSE 전송 오류", guid, e);
                emitter.completeWithError(e);
            }
        }, sseTaskExecutor);

        return emitter;
    }

    /**
     * Step 5 — 계약 서류 조회 (BK-B20)
     */
    public LoanContractDocumentsResponse getContractDocuments(
            String loanProductCode, String evaluationId, Long staffId) {

        String guid = resolveGuidForEval(evaluationId);
        log.info("[{}] 계약 서류 조회 - staffId: {}, evaluationId: {}, productCode: {}",
                guid, staffId, evaluationId, loanProductCode);

        LoanContractDocumentsResponse response = buildMockContractDocuments(loanProductCode);

        log.info("[{}] 계약 서류 조회 완료", guid);
        return response;
    }

    /**
     * Step 6 — 대출 실행 (BK-B21~B23)
     */
    public LoanExecuteResponse executeLoan(LoanExecuteRequest request, Long staffId) {
        String guid = resolveGuidForEval(request.getEvaluationId());
        log.info("[{}] 대출 실행 요청 - staffId: {}, evaluationId: {}, executeAmount: {}",
                guid, staffId, request.getEvaluationId(), request.getExecuteAmount());

        LoanExecuteResponse response = buildMockLoanExecuteResponse(request.getExecuteAmount(), request.getRepaymentPeriod());

        log.info("[{}] 대출 실행 완료 - loanId: {}", guid, response.getLoanId());
        return response;
    }

    // ─── Private helpers ───────────────────────────────────────────────────────

    private String generateGuid() {
        return LOAN_GUID_PREFIX + UUID.randomUUID().toString().toUpperCase();
    }

    private String generateApplicationId() {
        return "APP-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
    }

    private String generateEvaluationId() {
        return "EVAL-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
    }

    private String resolveGuidForApp(String applicationId) {
        String guid = redisTemplate.opsForValue().get(String.format(REDIS_APP_GUID_KEY, applicationId));
        if (guid == null) {
            log.warn("applicationId {}에 대한 GUID 없음 — 미등록 신청", applicationId);
            throw new BusinessException(ErrorCode.LOAN_EVALUATION_NOT_FOUND);
        }
        return guid;
    }

    private String resolveEvaluationIdForApp(String applicationId) {
        String evaluationId = redisTemplate.opsForValue().get(String.format(REDIS_APP_EVAL_KEY, applicationId));
        if (evaluationId == null) {
            log.warn("applicationId {}에 대한 evaluationId 없음 — 미등록 신청", applicationId);
            throw new BusinessException(ErrorCode.LOAN_EVALUATION_NOT_FOUND);
        }
        return evaluationId;
    }

    private String resolveGuidForEval(String evaluationId) {
        String guid = redisTemplate.opsForValue().get(String.format(REDIS_EVAL_GUID_KEY, evaluationId));
        if (guid == null) {
            log.warn("evaluationId {}에 대한 GUID 없음 — 미등록 심사", evaluationId);
            throw new BusinessException(ErrorCode.LOAN_EVALUATION_NOT_FOUND);
        }
        return guid;
    }

    private String maskName(String name) {
        if (name == null || name.length() < 2) return "**";
        return name.charAt(0) + "*".repeat(name.length() - 1);
    }

    // ─── Mock 응답 빌더 (실제 mTLS 연동 시 BankLoanClient로 교체) ───────────────

    private LoanRequiredDocumentsResponse buildMockRequiredDocuments() {
        return LoanRequiredDocumentsResponse.builder()
                .documents(List.of(
                        TermsDocumentDto.builder()
                                .documentType("T001")
                                .documentName("신용정보 조회 동의서")
                                .documentUrl("https://cdn.bank.example/terms/T001.html")
                                .isMandatory(true)
                                .build(),
                        TermsDocumentDto.builder()
                                .documentType("T002")
                                .documentName("개인정보 수집·이용 동의서")
                                .documentUrl("https://cdn.bank.example/terms/T002.html")
                                .isMandatory(true)
                                .build(),
                        TermsDocumentDto.builder()
                                .documentType("T003")
                                .documentName("소득확인 동의서")
                                .documentUrl("https://cdn.bank.example/terms/T003.pdf")
                                .isMandatory(false)
                                .build()
                ))
                .build();
    }

    private LoanEvaluationResultResponse buildMockEvaluationResult(String applicationId, String evaluationId) {
        String now = LocalDateTime.now().toString();
        return LoanEvaluationResultResponse.builder()
                .applicationId(applicationId)
                .evaluationStatus("APPROVED")
                .requestedAt(now)
                .completedAt(now)
                .evaluationId(evaluationId)
                .approvedLimit(new BigDecimal("30000000"))
                .availableProducts(List.of(
                        AvailableProductDto.builder()
                                .loanProductCode("LP004")
                                .loanProductName("우리 프리미엄 직장인 대출")
                                .minAmount(new BigDecimal("5000000"))
                                .maxAmount(new BigDecimal("50000000"))
                                .interestRate(new BigDecimal("6.50"))
                                .loanPeriodMonths(60)
                                .build(),
                        AvailableProductDto.builder()
                                .loanProductCode("LP001")
                                .loanProductName("우리 직장인 신용대출")
                                .minAmount(new BigDecimal("1000000"))
                                .maxAmount(new BigDecimal("30000000"))
                                .interestRate(new BigDecimal("4.50"))
                                .loanPeriodMonths(36)
                                .build(),
                        AvailableProductDto.builder()
                                .loanProductCode("LP003")
                                .loanProductName("우리 스마트론")
                                .minAmount(new BigDecimal("1000000"))
                                .maxAmount(new BigDecimal("15000000"))
                                .interestRate(new BigDecimal("3.80"))
                                .loanPeriodMonths(24)
                                .build(),
                        AvailableProductDto.builder()
                                .loanProductCode("LP005")
                                .loanProductName("우리 직장인 플러스론")
                                .minAmount(new BigDecimal("1000000"))
                                .maxAmount(new BigDecimal("20000000"))
                                .interestRate(new BigDecimal("4.20"))
                                .loanPeriodMonths(36)
                                .build(),
                        AvailableProductDto.builder()
                                .loanProductCode("LP002")
                                .loanProductName("우리 든든 직장인 대출")
                                .minAmount(new BigDecimal("1000000"))
                                .maxAmount(new BigDecimal("25000000"))
                                .interestRate(new BigDecimal("5.20"))
                                .loanPeriodMonths(48)
                                .build()
                ))
                .build();
    }

    private LoanContractDocumentsResponse buildMockContractDocuments(String loanProductCode) {
        String productName = PRODUCT_NAME_MAP.getOrDefault(loanProductCode, "알 수 없는 상품");
        return LoanContractDocumentsResponse.builder()
                .loanProductCode(loanProductCode)
                .loanProductName(productName)
                .approvedLimit(new BigDecimal("30000000"))
                .documents(List.of(
                        TermsDocumentDto.builder()
                                .documentType("C001")
                                .documentName("대출거래약정서")
                                .documentUrl("https://cdn.bank.example/contract/C001.pdf")
                                .isMandatory(true)
                                .build(),
                        TermsDocumentDto.builder()
                                .documentType("C002")
                                .documentName("상품설명서")
                                .documentUrl("https://cdn.bank.example/contract/C002.pdf")
                                .isMandatory(true)
                                .build(),
                        TermsDocumentDto.builder()
                                .documentType("C003")
                                .documentName("금리인하요구권 안내서")
                                .documentUrl("https://cdn.bank.example/contract/C003.html")
                                .isMandatory(false)
                                .build()
                ))
                .build();
    }

    private LoanExecuteResponse buildMockLoanExecuteResponse(BigDecimal executeAmount, int repaymentPeriod) {
        BigDecimal interestRate = new BigDecimal("4.50");
        BigDecimal monthlyPayment = calculateMonthlyPayment(executeAmount, interestRate, repaymentPeriod);
        String repaymentStartDate = LocalDate.now().plusMonths(1).toString();
        String maturityDate = LocalDate.now().plusMonths(repaymentPeriod).toString();
        return LoanExecuteResponse.builder()
                .loanId("LOAN-" + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase())
                .borrowerName("고객")
                .depositTransactionId("TXN-" + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase())
                .loanBalance(executeAmount)
                .executeAmount(executeAmount)
                .interestRate(interestRate)
                .repaymentPeriod(repaymentPeriod)
                .monthlyPayment(monthlyPayment)
                .repaymentStartDate(repaymentStartDate)
                .maturityDate(maturityDate)
                .build();
    }

    /**
     * 원리금균등 월 상환금 계산: M = P * r(1+r)^n / ((1+r)^n - 1)
     * double 대신 BigDecimal 사용 — 금융 계산에서 부동소수점 오차를 방지한다.
     * 무이자(annualRate=0)이면 원금을 개월 수로 나눈 값을 반환한다.
     */
    private BigDecimal calculateMonthlyPayment(BigDecimal principal, BigDecimal annualRate, int months) {
        if (months <= 0) return principal;

        if (annualRate.compareTo(BigDecimal.ZERO) == 0) {
            return principal.divide(BigDecimal.valueOf(months), 0, java.math.RoundingMode.HALF_UP);
        }

        BigDecimal monthlyRate = annualRate.divide(BigDecimal.valueOf(1200), 10, java.math.RoundingMode.HALF_UP);
        BigDecimal onePlusR = BigDecimal.ONE.add(monthlyRate);
        BigDecimal pow = onePlusR.pow(months);

        BigDecimal numerator = principal.multiply(monthlyRate).multiply(pow);
        BigDecimal denominator = pow.subtract(BigDecimal.ONE);

        return numerator.divide(denominator, 0, java.math.RoundingMode.HALF_UP);
    }
}

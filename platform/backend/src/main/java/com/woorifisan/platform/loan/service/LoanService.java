package com.woorifisan.platform.loan.service;

import com.woorifisan.platform.loan.dto.AvailableProductDto;
import com.woorifisan.platform.loan.dto.LoanContractDocumentsResponse;
import com.woorifisan.platform.loan.dto.LoanEvaluateRequest;
import com.woorifisan.platform.loan.dto.LoanEvaluateResponse;
import com.woorifisan.platform.loan.dto.LoanEvaluationResultResponse;
import com.woorifisan.platform.loan.dto.LoanExecuteRequest;
import com.woorifisan.platform.loan.dto.LoanExecuteResponse;
import com.woorifisan.platform.loan.dto.LoanRequiredDocumentsResponse;
import com.woorifisan.platform.loan.dto.TermsDocumentDto;
import com.woorifisan.platform.global.exception.BusinessException;
import com.woorifisan.platform.global.response.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoanService {

    private static final String LOAN_GUID_PREFIX = "LN-";
    private static final String REDIS_EVAL_GUID_KEY = "loan:eval:%s:guid";
    private static final String REDIS_APP_GUID_KEY = "loan:app:%s:guid";
    private static final String REDIS_APP_EVAL_KEY = "loan:app:%s:evalId";
    private static final long GUID_TTL_HOURS = 24L;

    private final StringRedisTemplate redisTemplate;

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
        boolean allMandatoryAgreed = request.getDocuments().stream()
                .allMatch(d -> d.getDocumentType() != null && !d.getDocumentType().isBlank()
                        && d.getAgreedAt() != null && !d.getAgreedAt().isBlank());
        if (!allMandatoryAgreed) {
            throw new BusinessException(ErrorCode.LOAN_TERMS_NOT_AGREED);
        }

        String guid = generateGuid();
        String applicationId = generateApplicationId();
        String evaluationId = generateEvaluationId();
        LocalDateTime receivedAt = LocalDateTime.now();

        log.info("[{}] 대출 심사 요청 - staffId: {}, customerName: {}, applicationId: {}",
                guid, staffId, maskName(request.getCustomerName()), applicationId);

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
     * Step 3 — 심사 결과 조회 (Polling) (BK-B19)
     * 동일 applicationId에 대한 GUID 중복 생성 방지 — 최초 GUID 재사용
     */
    public LoanEvaluationResultResponse getEvaluationResult(String applicationId, Long staffId) {
        String guid = resolveGuidForApp(applicationId);
        String evaluationId = resolveEvaluationIdForApp(applicationId);

        log.info("[{}] 심사 결과 조회 - staffId: {}, applicationId: {}", guid, staffId, applicationId);

        LoanEvaluationResultResponse response = buildMockEvaluationResult(applicationId, evaluationId);

        log.info("[{}] 심사 결과 조회 완료 - status: {}", guid, response.getEvaluationStatus());
        return response;
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

        LoanExecuteResponse response = buildMockLoanExecuteResponse(request.getExecuteAmount());

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
                .interestRate(new BigDecimal("4.50"))
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
        String productName = "LP001".equals(loanProductCode) ? "우리 직장인 신용대출" : "우리 든든 직장인 대출";
        return LoanContractDocumentsResponse.builder()
                .loanProductCode(loanProductCode)
                .loanProductName(productName)
                .approvedLimit(new BigDecimal("30000000"))
                .documentUrl("https://cdn.bank.example/contract/")
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

    private LoanExecuteResponse buildMockLoanExecuteResponse(BigDecimal executeAmount) {
        BigDecimal interestRate = new BigDecimal("4.50");
        String repaymentStartDate = LocalDate.now().plusMonths(1).toString();
        String maturityDate = LocalDate.now().plusYears(3).toString();
        return LoanExecuteResponse.builder()
                .loanId("LOAN-" + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase())
                .borrowerName("고객")
                .depositTransactionId("TXN-" + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase())
                .loanBalance(executeAmount)
                .executeAmount(executeAmount)
                .interestRate(interestRate)
                .repaymentStartDate(repaymentStartDate)
                .maturityDate(maturityDate)
                .build();
    }
}

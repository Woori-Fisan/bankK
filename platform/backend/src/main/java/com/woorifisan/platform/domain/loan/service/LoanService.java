package com.woorifisan.platform.domain.loan.service;

import com.woorifisan.platform.global.util.MaskingUtil;
import com.woorifisan.platform.domain.loan.dto.request.LoanContractDocumentsRequest;
import com.woorifisan.platform.domain.loan.dto.response.LoanContractDocumentsResponse;
import com.woorifisan.platform.domain.loan.dto.request.LoanEvaluateRequest;
import com.woorifisan.platform.domain.loan.dto.response.LoanEvaluateResponse;
import com.woorifisan.platform.domain.loan.dto.response.LoanEvaluationResultResponse;
import com.woorifisan.platform.domain.loan.dto.request.LoanExecuteRequest;
import com.woorifisan.platform.domain.loan.dto.response.LoanExecuteResponse;
import com.woorifisan.platform.domain.loan.dto.response.LoanProductDto;
import com.woorifisan.platform.domain.loan.dto.request.LoanProductSelectRequest;
import com.woorifisan.platform.domain.loan.dto.response.LoanProductSelectResponse;
import com.woorifisan.platform.domain.loan.dto.request.LoanRequiredDocumentsRequest;
import com.woorifisan.platform.domain.loan.dto.response.LoanRequiredDocumentsResponse;
import com.woorifisan.platform.domain.loan.dto.response.TermsDocumentDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoanService {

    private static final String LOAN_GUID_PREFIX = "LN-";
    private static final String REDIS_EVAL_GUID_KEY_FORMAT = "loan:eval:%s:guid";
    private static final long EVAL_GUID_TTL_HOURS = 24L;

    private final StringRedisTemplate redisTemplate;

    /**
     * Step 1 — 심사 서류 조회 (BK-B11)
     */
    public LoanRequiredDocumentsResponse getRequiredDocuments(
            LoanRequiredDocumentsRequest request, Long staffId) {

        String guid = generateGuid();
        LocalDateTime requestAt = LocalDateTime.now();
        log.info("[{}] 심사 서류 조회 요청 - staffId: {}, bankCode: {}, requestAt: {}",
                guid, staffId, request.getBankCode(), requestAt);

        LoanRequiredDocumentsResponse response = buildMockRequiredDocuments();

        log.info("[{}] 심사 서류 조회 완료 - responseAt: {}", guid, LocalDateTime.now());
        return response;
    }

    /**
     * Step 2 — 서류 제출 및 심사 요청 (BK-B12~B19)
     * 최초 GUID를 생성하여 Redis에 저장 → 이후 Polling 시 재사용
     */
    public LoanEvaluateResponse evaluateLoan(LoanEvaluateRequest request, Long staffId) {
        String guid = generateGuid();
        LocalDateTime requestAt = LocalDateTime.now();
        log.info("[{}] 대출 심사 요청 - staffId: {}, bankCode: {}, holder: {}, requestAt: {}",
                guid, staffId, request.getBankCode(),
                MaskingUtil.maskName(request.getDepositAccountHolder()), requestAt);

        // Mock: 은행 코어로 Pass-through 후 evaluationId 수신
        String evaluationId = generateEvaluationId();

        // 동일 evaluationId Polling 시 GUID 재사용을 위해 Redis에 저장
        storeGuidForEvaluation(evaluationId, guid);

        log.info("[{}] 대출 심사 요청 접수 완료 - evaluationId: {}, responseAt: {}",
                guid, evaluationId, LocalDateTime.now());
        return LoanEvaluateResponse.builder()
                .evaluationId(evaluationId)
                .status("SUBMITTED")
                .message("심사 요청이 접수되었습니다.")
                .build();
    }

    /**
     * Step 3 — 심사 결과 조회 (Polling) (BK-B19)
     * 동일 evaluationId에 대한 GUID 중복 생성 방지 — 최초 GUID 재사용
     */
    public LoanEvaluationResultResponse getEvaluationResult(
            String evaluationId, String bankCode, Long staffId) {

        // 최초 심사 요청 시 생성된 GUID 재사용
        String guid = resolveGuidForEvaluation(evaluationId);
        LocalDateTime requestAt = LocalDateTime.now();
        log.info("[{}] 심사 결과 조회 - staffId: {}, evaluationId: {}, bankCode: {}, requestAt: {}",
                guid, staffId, evaluationId, bankCode, requestAt);

        LoanEvaluationResultResponse response = buildMockEvaluationResult(evaluationId);

        log.info("[{}] 심사 결과 조회 완료 - status: {}, responseAt: {}",
                guid, response.getStatus(), LocalDateTime.now());
        return response;
    }

    /**
     * Step 4 — 상품 선택 (BK-B18/B27)
     */
    public LoanProductSelectResponse selectProduct(LoanProductSelectRequest request, Long staffId) {
        String guid = resolveGuidForEvaluation(request.getEvaluationId());
        LocalDateTime requestAt = LocalDateTime.now();
        log.info("[{}] 상품 선택 - staffId: {}, evaluationId: {}, productCode: {}, period: {}, requestAt: {}",
                guid, staffId, request.getEvaluationId(), request.getProductCode(),
                request.getPeriod(), requestAt);

        LoanProductSelectResponse response = buildMockProductSelectResponse(request);

        log.info("[{}] 상품 선택 완료 - responseAt: {}", guid, LocalDateTime.now());
        return response;
    }

    /**
     * Step 5 — 계약 서류 조회 (BK-B20)
     */
    public LoanContractDocumentsResponse getContractDocuments(
            LoanContractDocumentsRequest request, Long staffId) {

        String guid = resolveGuidForEvaluation(request.getEvaluationId());
        LocalDateTime requestAt = LocalDateTime.now();
        log.info("[{}] 계약 서류 조회 - staffId: {}, evaluationId: {}, productCode: {}, requestAt: {}",
                guid, staffId, request.getEvaluationId(), request.getProductCode(), requestAt);

        LoanContractDocumentsResponse response = buildMockContractDocuments();

        log.info("[{}] 계약 서류 조회 완료 - responseAt: {}", guid, LocalDateTime.now());
        return response;
    }

    /**
     * Step 6 — 대출 실행 (BK-B21~B23)
     */
    public LoanExecuteResponse executeLoan(LoanExecuteRequest request, Long staffId) {
        String guid = resolveGuidForEvaluation(request.getEvaluationId());
        LocalDateTime requestAt = LocalDateTime.now();
        log.info("[{}] 대출 실행 요청 - staffId: {}, evaluationId: {}, productCode: {}, period: {}, requestAt: {}",
                guid, staffId, request.getEvaluationId(), request.getProductCode(),
                request.getPeriod(), requestAt);

        LoanExecuteResponse response = buildMockLoanExecuteResponse();

        log.info("[{}] 대출 실행 완료 - loanNo: {}, responseAt: {}",
                guid, response.getLoanNo(), LocalDateTime.now());
        return response;
    }

    // ─── Private helpers ───────────────────────────────────────────────────────

    private String generateGuid() {
        return LOAN_GUID_PREFIX + UUID.randomUUID().toString().toUpperCase();
    }

    private String generateEvaluationId() {
        return "EVAL-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
    }

    private void storeGuidForEvaluation(String evaluationId, String guid) {
        String redisKey = String.format(REDIS_EVAL_GUID_KEY_FORMAT, evaluationId);
        redisTemplate.opsForValue().set(redisKey, guid, EVAL_GUID_TTL_HOURS, TimeUnit.HOURS);
    }

    /**
     * evaluationId에 매핑된 GUID 조회.
     * Redis TTL 만료 등 비정상 상황에서는 신규 GUID를 생성하고 다시 저장한다.
     */
    private String resolveGuidForEvaluation(String evaluationId) {
        String redisKey = String.format(REDIS_EVAL_GUID_KEY_FORMAT, evaluationId);
        String guid = redisTemplate.opsForValue().get(redisKey);
        if (guid == null) {
            guid = generateGuid();
            redisTemplate.opsForValue().set(redisKey, guid, EVAL_GUID_TTL_HOURS, TimeUnit.HOURS);
            log.warn("evaluationId {}에 대한 GUID가 없어 신규 생성: {}", evaluationId, guid);
        }
        return guid;
    }

    // ─── Mock 응답 빌더 (실제 mTLS 연동 시 BankLoanClient로 교체) ───────────────

    private LoanRequiredDocumentsResponse buildMockRequiredDocuments() {
        return LoanRequiredDocumentsResponse.builder()
                .documentList(List.of(
                        TermsDocumentDto.builder()
                                .termsCode("T001")
                                .title("신용정보 조회 동의서")
                                .termsUrl("https://cdn.bank.example/terms/T001.html")
                                .contentType("HTML")
                                .isMandatory(true)
                                .build(),
                        TermsDocumentDto.builder()
                                .termsCode("T002")
                                .title("개인정보 수집·이용 동의서")
                                .termsUrl("https://cdn.bank.example/terms/T002.html")
                                .contentType("HTML")
                                .isMandatory(true)
                                .build(),
                        TermsDocumentDto.builder()
                                .termsCode("T003")
                                .title("소득확인 동의서")
                                .termsUrl("https://cdn.bank.example/terms/T003.pdf")
                                .contentType("PDF")
                                .isMandatory(false)
                                .build()
                ))
                .build();
    }

    private LoanEvaluationResultResponse buildMockEvaluationResult(String evaluationId) {
        return LoanEvaluationResultResponse.builder()
                .evaluationId(evaluationId)
                .status("APPROVED")
                .approvedLimit(new BigDecimal("30000000"))
                .interestRate(new BigDecimal("4.50"))
                .productList(List.of(
                        LoanProductDto.builder()
                                .productCode("LP001")
                                .productName("우리 직장인 신용대출")
                                .interestRate(new BigDecimal("4.50"))
                                .limit(new BigDecimal("30000000"))
                                .repaymentAmountByPeriod(Map.of(
                                        12, new BigDecimal("2562500"),
                                        24, new BigDecimal("1299167"),
                                        36, new BigDecimal("884028")
                                ))
                                .build(),
                        LoanProductDto.builder()
                                .productCode("LP002")
                                .productName("우리 든든 직장인 대출")
                                .interestRate(new BigDecimal("5.20"))
                                .limit(new BigDecimal("25000000"))
                                .repaymentAmountByPeriod(Map.of(
                                        12, new BigDecimal("2141667"),
                                        24, new BigDecimal("1086458"),
                                        36, new BigDecimal("739236")
                                ))
                                .build()
                ))
                .build();
    }

    private LoanProductSelectResponse buildMockProductSelectResponse(LoanProductSelectRequest request) {
        BigDecimal loanAmount = new BigDecimal("20000000");
        BigDecimal interestRate = new BigDecimal("4.50");
        BigDecimal monthlyRate = interestRate.divide(BigDecimal.valueOf(1200), 10, RoundingMode.HALF_UP);
        BigDecimal monthlyPayment;
        if (monthlyRate.compareTo(BigDecimal.ZERO) == 0) {
            monthlyPayment = loanAmount.divide(BigDecimal.valueOf(request.getPeriod()), 0, RoundingMode.HALF_UP);
        } else {
            BigDecimal pow = monthlyRate.add(BigDecimal.ONE).pow(request.getPeriod());
            monthlyPayment = loanAmount
                    .multiply(monthlyRate)
                    .multiply(pow)
                    .divide(pow.subtract(BigDecimal.ONE), 0, RoundingMode.HALF_UP);
        }

        return LoanProductSelectResponse.builder()
                .productCode(request.getProductCode())
                .productName("우리 직장인 신용대출")
                .loanAmount(loanAmount)
                .interestRate(interestRate)
                .period(request.getPeriod())
                .repaymentType("원리금균등")
                .monthlyPayment(monthlyPayment)
                .build();
    }

    private LoanContractDocumentsResponse buildMockContractDocuments() {
        return LoanContractDocumentsResponse.builder()
                .approvedLimit(new BigDecimal("20000000"))
                .interestRate(new BigDecimal("4.50"))
                .documentList(List.of(
                        TermsDocumentDto.builder()
                                .termsCode("C001")
                                .title("대출거래약정서")
                                .termsUrl("https://cdn.bank.example/contract/C001.pdf")
                                .contentType("PDF")
                                .isMandatory(true)
                                .build(),
                        TermsDocumentDto.builder()
                                .termsCode("C002")
                                .title("상품설명서")
                                .termsUrl("https://cdn.bank.example/contract/C002.pdf")
                                .contentType("PDF")
                                .isMandatory(true)
                                .build(),
                        TermsDocumentDto.builder()
                                .termsCode("C003")
                                .title("금리인하요구권 안내서")
                                .termsUrl("https://cdn.bank.example/contract/C003.html")
                                .contentType("HTML")
                                .isMandatory(false)
                                .build()
                ))
                .build();
    }

    private LoanExecuteResponse buildMockLoanExecuteResponse() {
        return LoanExecuteResponse.builder()
                .loanNo("LN-" + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase())
                .loanAmount(new BigDecimal("20000000"))
                .applicantName("김우리")
                .depositAccountNo(MaskingUtil.maskAccountNo("1002345678901"))
                .interestRate(new BigDecimal("4.50"))
                .endDate("2029-05-19")
                .build();
    }
}

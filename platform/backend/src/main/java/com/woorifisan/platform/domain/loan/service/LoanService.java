package com.woorifisan.platform.domain.loan.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.woorifisan.platform.domain.bank.external.client.BankLoanClient;
import com.woorifisan.platform.domain.bank.external.dto.BankLoanEvaluateRequest;
import com.woorifisan.platform.domain.bank.external.dto.BankLoanExecuteRequest;
import com.woorifisan.platform.domain.bank.mapper.BankMapper;
import com.woorifisan.platform.domain.bank.model.Bank;
import com.woorifisan.platform.domain.loan.dto.request.LoanCallbackRequest;
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
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoanService {

    // SSE 연결을 60초 유지. Bank 비동기 심사가 이 안에 끝나야 프론트에 결과 전달 가능
    private static final long SSE_TIMEOUT_MS = 60_000L;

    // requestKey → SseEmitter 매핑 테이블.
    // static: 인스턴스가 여러 개여도(멀티스레드 환경) 동일한 Map을 공유해야 하기 때문
    // ConcurrentHashMap: 여러 스레드(HTTP 요청 스레드, Webhook 수신 스레드)가 동시에 put/remove해도 안전
    private static final ConcurrentHashMap<String, SseEmitter> pendingEmitters = new ConcurrentHashMap<>();

    @Value("${bank.webhook.secret}")
    private String webhookSecret;

    private final BankLoanClient bankLoanClient;
    private final BankMapper bankMapper;
    private final ObjectMapper objectMapper;

    // SSE 구독
    public SseEmitter subscribe(String requestKey) {
        // SSE_TIMEOUT_MS 후 자동 만료되는 emitter 생성
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
        // 프론트가 /subscribe 호출 → emitter를 Map에 등록
        pendingEmitters.put(requestKey, emitter);

        // 60초가 지나도 Bank webhook이 안 오면 timeout 이벤트 전송 후 연결 종료
        emitter.onTimeout(() -> {
            pendingEmitters.remove(requestKey);
            try {
                emitter.send(SseEmitter.event().name("timeout").data("{\"status\":\"TIMEOUT\"}"));
            } catch (Exception ignored) {}
            emitter.complete();
        });
        // handleCallback()에서 emitter.complete() 호출 시 Map에서 제거
        emitter.onCompletion(() -> pendingEmitters.remove(requestKey));
        // 브라우저가 탭을 닫거나 네트워크 오류 시 Map에서 제거
        emitter.onError(e -> {
            pendingEmitters.remove(requestKey);
            log.warn("[SSE] 연결 오류 - requestKey: {}", requestKey);
        });

        log.info("[SSE] 구독 등록 - requestKey: {}", requestKey);
        return emitter;
    }

    // 심사 서류 조회
    public LoanRequiredDocumentsResponse getRequiredDocuments(Long staffId) {
        log.info("[심사서류] 조회 요청 - staffId: {}", staffId);
        List<TermsDocumentDto> documents = bankLoanClient.getEvaluationTerms().stream()
                .map(terms -> TermsDocumentDto.builder()
                        .documentType(Objects.toString(terms.get("termsCode"), null))
                        .documentName(Objects.toString(terms.get("title"), null))
                        .documentUrl(Objects.toString(terms.get("termsUrl"), null))
                        .isMandatory(Boolean.TRUE.equals(terms.get("mandatory")))
                        .build())
                .toList();
        return LoanRequiredDocumentsResponse.builder().documents(documents).build();
    }

    // 심사 신청
    public LoanEvaluateResponse evaluateLoan(LoanEvaluateRequest request,
                                             List<MultipartFile> files,
                                             Long staffId) {
        // 1. 요청한 은행코드가 활성화된 은행인지 확인
        Bank bank = bankMapper.findByBankCode(request.getBankCode())
                .filter(Bank::isActive)
                .orElseThrow(() -> new BusinessException(ErrorCode.BANK_NOT_FOUND));

        // 2. 입금 계좌 은행코드가 심사 신청 은행과 동일한지 확인 (타행 계좌로 입금 불가)
        if (!bank.getBankCode().equals(request.getDepositBankCode())) {
            throw new BusinessException(ErrorCode.LOAN_DEPOSIT_BANK_MISMATCH);
        }

        log.info("[심사신청] 은행 API 전달 시작 - requestKey: {}, staffId: {}, bankCode: {}",
                request.getRequestKey(), staffId, request.getBankCode());

        // 3. Platform DTO → Bank 전용 DTO 변환
        BankLoanEvaluateRequest bankData = BankLoanEvaluateRequest.builder()
                .requestKey(request.getRequestKey())
                .customerName(request.getCustomerName())
                .customerRrnPrefix(request.getCustomerRrnPrefix())
                .depositBankCode(request.getDepositBankCode())
                .depositAccountNo(request.getDepositAccountNo())
                // 프론트에서 금액/기간을 안 보낸 경우 기본값 적용 (1억 / 60개월)
                .requestedAmount(request.getRequestedAmount() != null
                        ? request.getRequestedAmount() : new BigDecimal("100000000"))
                .requestedPeriod(request.getRequestedPeriod() != null
                        ? request.getRequestedPeriod() : 60)
                .creditInfoAgreed(hasAgreed(request, "CREDIT_INFO_AGREE"))
                .productTermsAgreed(hasAgreed(request, "NICE_CREDIT_INQUIRY"))
                .documentCollected(hasAgreed(request, "DOCUMENT_COLLECT"))
                .build();

        // 4. Bank API 호출 (multipart pass-through — 파일은 메모리에서 직접 전달, 디스크 저장 없음)
        Map<String, Object> data = bankLoanClient.submitEvaluation(bankData, files);
        String loanNo = Objects.toString(data.get("loanNo"), null);
        String status = Objects.toString(data.get("status"), "SUBMITTED");
        log.info("[심사신청] 접수 완료 - loanNo: {}, requestKey: {}", loanNo, request.getRequestKey());
        // 프론트에는 loanNo + SUBMITTED 만 반환. 심사 결과는 SSE로 별도 수신
        return new LoanEvaluateResponse(loanNo, status);
    }

    // Webhook 수신 처리
    public void handleCallback(LoanCallbackRequest callback, String secret) {
        // 1. X-Webhook-Secret 헤더 검증 — 위조 요청 차단
        if (!webhookSecret.equals(secret)) {
            log.warn("[Webhook] 인증 실패 - requestKey: {}", callback.getRequestKey());
            throw new BusinessException(ErrorCode.LOAN_WEBHOOK_SECRET_INVALID);
        }

        String requestKey = callback.getRequestKey();
        if (requestKey == null) {
            log.warn("[Webhook] requestKey 누락 - loanNo: {}", callback.getLoanNo());
            return;
        }
        // 2. Map에서 emitter 꺼내기 (이후 중복 webhook이 와도 처리 안 함)
        SseEmitter emitter = pendingEmitters.remove(requestKey);
        if (emitter == null) {
            // 60초 타임아웃으로 이미 만료된 경우 — 정상적인 케이스이므로 에러 아님
            log.warn("[Webhook] SSE 에미터 없음 (이미 만료) - requestKey: {}", requestKey);
            return;
        }

        try {
            // 3. Webhook 데이터를 프론트 형식으로 변환
            LoanEvaluationResultResponse result = buildSseResult(callback);
            // 4. SSE result 이벤트로 프론트에 심사 결과 전송
            emitter.send(SseEmitter.event().name("result")
                    .data(objectMapper.writeValueAsString(result)));
            // 5. 연결 종료 → onCompletion 콜백이 Map 정리
            emitter.complete();
            log.info("[Webhook] SSE 전송 완료 - loanNo: {}, status: {}", callback.getLoanNo(), callback.getStatus());
        } catch (Exception e) {
            log.error("[Webhook] SSE 전송 실패 - requestKey: {}", requestKey, e);
            emitter.completeWithError(e);
        }
    }

    // Bank webhook 데이터 → 프론트 SSE 응답 DTO 변환
    private LoanEvaluationResultResponse buildSseResult(LoanCallbackRequest callback) {
        var builder = LoanEvaluationResultResponse.builder()
                .evaluationStatus(callback.getStatus())
                .evaluationId(callback.getLoanNo());

        if ("APPROVED".equals(callback.getStatus())) {
            // APPROVED: 승인 한도 + 선택 가능한 상품 목록 포함
            List<AvailableProductDto> products = callback.getAvailableProducts() == null
                    ? List.of()
                    : callback.getAvailableProducts().stream()
                            .map(p -> AvailableProductDto.builder()
                                    .loanProductCode(Objects.toString(p.get("productId"), null))
                                    .loanProductName(Objects.toString(p.get("productName"), null))
                                    .minAmount(toBigDecimal(p.get("minLimit")))
                                    .maxAmount(toBigDecimal(p.get("maxLimit")))
                                    .interestRate(toBigDecimal(p.get("minRate")))
                                    .loanPeriodMonths(36)
                                    .build())
                            .toList();
            builder.approvedLimit(callback.getApprovedLimit())
                   .availableProducts(products);
        } else {
            // REJECTED / SYSTEM_ERROR: 거절 사유 포함
            builder.rejectionMessage(callback.getRejectReason());
        }

        return builder.build();
    }

    // 계약 서류 조회
    public LoanContractDocumentsResponse getContractDocuments(String loanProductCode,
                                                               String loanNo,
                                                               Long staffId) {
        log.info("[계약서류] 조회 - staffId: {}, loanNo: {}, productCode: {}", staffId, loanNo, loanProductCode);
        List<TermsDocumentDto> documents = bankLoanClient.getContractTerms(loanProductCode, loanNo).stream()
                .map(terms -> TermsDocumentDto.builder()
                        .documentType(Objects.toString(terms.get("termsCode"), null))
                        .documentName(Objects.toString(terms.get("title"), null))
                        .documentUrl(Objects.toString(terms.get("termsUrl"), null))
                        .isMandatory(Boolean.TRUE.equals(terms.get("mandatory")))
                        .build())
                .toList();
        return LoanContractDocumentsResponse.builder()
                .loanProductCode(loanProductCode)
                .loanProductName("심사 승인 상품")
                .documents(documents)
                .build();
    }

    // 대출 실행
    public LoanExecuteResponse executeLoan(LoanExecuteRequest request, Long staffId) {
        log.info("[대출실행] 요청 - staffId: {}, loanNo: {}, amount: {}",
                staffId, request.getEvaluationId(), request.getExecuteAmount());

        // loanProductCode는 프론트에서 String으로 넘어오지만, Bank API는 Long productId를 기대함
        long productId;
        try {
            productId = Long.parseLong(request.getLoanProductCode());
        } catch (NumberFormatException e) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }

        // Platform DTO → Bank 전용 DTO 변환
        BankLoanExecuteRequest bankRequest = BankLoanExecuteRequest.builder()
                .loanNo(request.getEvaluationId())
                .productId(productId)
                .loanAmount(request.getExecuteAmount())
                .repaymentPeriod(request.getRepaymentPeriod())
                .repaymentType("원리금균등")
                .accountPassword(request.getAccountPassword())
                .build();

        Map<String, Object> data = bankLoanClient.executeLoan(bankRequest);
        log.info("[대출실행] 완료 - loanNo: {}", data.get("loanNo"));

        // Bank 응답 → Platform 응답 DTO 변환
        return LoanExecuteResponse.builder()
                .loanId(Objects.toString(data.get("loanNo"), null))
                .borrowerName(Objects.toString(data.get("customerName"), null))
                // 입금 거래번호: Bank가 별도 제공하지 않아 Platform에서 임의 생성
                .depositTransactionId("TXN-" + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase())
                .loanBalance(toBigDecimal(data.get("loanAmount")))
                .executeAmount(toBigDecimal(data.get("loanAmount")))
                .interestRate(toBigDecimal(data.get("interestRate")))
                .repaymentPeriod(data.get("repaymentPeriod") != null
                        ? Integer.parseInt(data.get("repaymentPeriod").toString()) : 0)
                .monthlyPayment(toBigDecimal(data.get("monthlyPayment")))
                .repaymentStartDate(Objects.toString(data.get("startDate"), null))
                .maturityDate(Objects.toString(data.get("endDate"), null))
                .build();
    }

    // Helpers
    // documents 리스트에서 특정 documentType 동의 여부 확인
    private boolean hasAgreed(LoanEvaluateRequest request, String type) {
        if (request.getDocuments() == null) return false;
        return request.getDocuments().stream().anyMatch(d -> type.equals(d.getDocumentType()));
    }

    // Bank가 Object로 반환한 숫자 값을 BigDecimal로 안전하게 변환
    private BigDecimal toBigDecimal(Object value) {
        if (value == null) return BigDecimal.ZERO;
        try { return new BigDecimal(value.toString()); } catch (NumberFormatException e) { return BigDecimal.ZERO; }
    }

}

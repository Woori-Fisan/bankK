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
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

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
        });

        // 연결 즉시 초기 이벤트 전송 — Nginx 등 프록시가 유휴 연결로 오인해 끊는 것을 방지
        try {
            emitter.send(SseEmitter.event().name("connect").data("connected"));
        } catch (Exception ignored) {}

        return emitter;
    }

    // 심사 서류 조회
    public LoanRequiredDocumentsResponse getRequiredDocuments(Long staffId) {
        List<TermsDocumentDto> documents = bankLoanClient.getEvaluationTerms().stream()
                .map(terms -> TermsDocumentDto.builder()
                        .documentType(Objects.toString(terms.get("termsCode"), null))
                        .documentName(Objects.toString(terms.get("title"), null))
                        .documentUrl(Objects.toString(terms.get("termsUrl"), null))
                        .documentContent(Objects.toString(terms.get("termsContent"), null))
                        .isMandatory(Boolean.TRUE.equals(terms.get("isMandatory")))
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

        // 3. Platform DTO → Bank 전용 DTO 변환 (Pass-through)
        BankLoanEvaluateRequest bankData = BankLoanEvaluateRequest.builder()
                .reqPayload(request.getReqPayload())
                .bankKeyId(request.getBankKeyId())
                .requestKey(request.getRequestKey())
                .depositBankCode(request.getDepositBankCode())
                // 프론트에서 금액/기간을 안 보낸 경우 기본값 적용
                .requestedAmount(request.getRequestedAmount() != null
                        ? request.getRequestedAmount() : new BigDecimal("100000000"))
                .requestedPeriod(request.getRequestedPeriod() != null
                        ? request.getRequestedPeriod() : 60)
                .creditInfoAgreed(hasAgreed(request, "CREDIT_INFO_AGREE"))
                .productTermsAgreed(hasAgreed(request, "NICE_CREDIT_INQUIRY"))
                .documentCollected(hasAgreed(request, "DOCUMENT_COLLECT"))
                .build();

        // 4. Bank API 호출 (multipart pass-through)
        Map<String, Object> data = bankLoanClient.submitEvaluation(bankData, files);
        String loanNo = Objects.toString(data.get("loanNo"), null);
        String status = Objects.toString(data.get("status"), "SUBMITTED");
        String resPayload = Objects.toString(data.get("resPayload"), null);

        return LoanEvaluateResponse.builder()
                .loanNo(loanNo)
                .status(status)
                .resPayload(resPayload)
                .build();
    }

    // Webhook 수신 처리
    public void handleCallback(LoanCallbackRequest callback, String secret) {
        // 1. X-Webhook-Secret 헤더 검증
        if (!webhookSecret.equals(secret)) {
            throw new BusinessException(ErrorCode.LOAN_WEBHOOK_SECRET_INVALID);
        }

        String requestKey = callback.getRequestKey();
        if (requestKey == null) {
            return;
        }
        // 2. Map에서 emitter 꺼내기
        SseEmitter emitter = pendingEmitters.remove(requestKey);
        if (emitter == null) {
            return;
        }

        try {
            // 3. Webhook 암호화 데이터를 그대로 담아 프론트 형식으로 변환 (Pass-through)
            LoanEvaluationResultResponse result = LoanEvaluationResultResponse.builder()
                    .evaluationStatus(callback.getStatus())
                    .evaluationId(callback.getLoanNo())
                    .resPayload(callback.getResPayload())
                    .build();
            
            // 4. SSE result 이벤트로 프론트에 심사 결과 전송
            emitter.send(SseEmitter.event().name("result")
                    .data(objectMapper.writeValueAsString(result)));
            
            // 5. 연결 종료
            emitter.complete();
        } catch (Exception e) {
            emitter.completeWithError(e);
        }
    }

    // 계약 서류 조회
    public LoanContractDocumentsResponse getContractDocuments(String loanProductCode,
                                                               String loanNo,
                                                               Long staffId) {
        List<TermsDocumentDto> documents = bankLoanClient.getContractTerms(loanProductCode, loanNo).stream()
                .map(terms -> TermsDocumentDto.builder()
                        .documentType(Objects.toString(terms.get("termsCode"), null))
                        .documentName(Objects.toString(terms.get("title"), null))
                        .documentUrl(Objects.toString(terms.get("termsUrl"), null))
                        .documentContent(Objects.toString(terms.get("termsContent"), null))
                        .isMandatory(Boolean.TRUE.equals(terms.get("isMandatory")))
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
        
        // Platform DTO → Bank 전용 DTO 변환 (Pass-through)
        BankLoanExecuteRequest bankRequest = BankLoanExecuteRequest.builder()
                .reqPayload(request.getReqPayload())
                .bankKeyId(request.getBankKeyId())
                .loanNo(request.getLoanNo())
                .productId(request.getProductId())
                .loanAmount(request.getExecuteAmount())
                .repaymentPeriod(request.getRepaymentPeriod())
                .repaymentType(request.getRepaymentType())
                .build();

        Map<String, Object> data = bankLoanClient.executeLoan(bankRequest);

        // Bank 응답 → Platform 응답 DTO 변환 (Pass-through)
        return LoanExecuteResponse.builder()
                .resPayload(Objects.toString(data.get("resPayload"), null))
                .loanNo(Objects.toString(data.get("loanNo"), null))
                .executeAmount(toBigDecimal(data.get("loanAmount")))
                .interestRate(toBigDecimal(data.get("interestRate")))
                .repaymentPeriod(data.get("repaymentPeriod") != null
                        ? Integer.parseInt(data.get("repaymentPeriod").toString()) : 0)
                .monthlyPayment(toBigDecimal(data.get("monthlyPayment")))
                .repaymentType(Objects.toString(data.get("repaymentType"), null))
                .startDate(Objects.toString(data.get("startDate"), null))
                .maturityDate(Objects.toString(data.get("endDate"), null))
                .linkedAccountId(data.get("linkedAccountId") != null
                        ? Long.parseLong(data.get("linkedAccountId").toString()) : null)
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
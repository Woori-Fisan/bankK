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
import com.woorifisan.platform.domain.loan.dto.request.LoanReceiptRequest;
import com.woorifisan.platform.domain.loan.dto.response.AvailableProductDto;
import com.woorifisan.platform.domain.loan.dto.response.LoanContractDocumentsResponse;
import com.woorifisan.platform.domain.loan.dto.response.LoanEvaluateResponse;
import com.woorifisan.platform.domain.loan.dto.response.LoanEvaluationResultResponse;
import com.woorifisan.platform.domain.loan.dto.response.LoanExecuteResponse;
import com.woorifisan.platform.domain.loan.dto.response.LoanRequiredDocumentsResponse;
import com.woorifisan.platform.domain.loan.dto.response.TermsDocumentDto;
import com.woorifisan.platform.global.exception.BusinessException;
import com.woorifisan.platform.global.response.ErrorCode;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
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

        // 연결 즉시 초기 이벤트 전송 — Nginx 등 프록시가 유휴 연결로 오인해 끊는 것을 방지
        try {
            emitter.send(SseEmitter.event().name("connect").data("connected"));
        } catch (Exception ignored) {}

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

    /**
     * 대출 실행 확인서 PDF 생성
     * Apache PDFBox를 사용해 A4 사이즈 PDF를 메모리에서 생성하고 바이트 배열로 반환
     */
    public byte[] generateReceiptPdf(LoanReceiptRequest req) {
        try (PDDocument doc = new PDDocument()) {

            // A4 사이즈 페이지 생성
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);

            // 클래스패스에서 NanumGothic 폰트 파일 로드
            InputStream fontStream = getClass().getResourceAsStream("/fonts/NanumGothic.ttf");
            if (fontStream == null) {
                throw new IllegalStateException("폰트 파일을 찾을 수 없습니다.");
            }

            // PDType0Font: TTF 파일을 PDF 내부에 임베드하는 CID 폰트 방식
            // PDType1Font(기본 14종)는 한글 미지원이므로 반드시 PDType0Font를 사용해야 함
            PDType0Font font = PDType0Font.load(doc, fontStream, true);

            float pageWidth = page.getMediaBox().getWidth();
            float margin = 55f;

            // PDPageContentStream: PDF 캔버스에 텍스트/선/도형을 그리는 객체
            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {

                // 헤더 영역
                drawCenteredText(cs, font, 15f, req.getDepositBankName(), pageWidth, 800f);
                drawCenteredText(cs, font, 19f, "대  출  실  행  확  인  서", pageWidth, 774f);

                // 헤더 구분선: 굵은 선(1.5pt) + 얇은 선(0.4pt)을 4포인트 간격으로 그려 이중선 효과
                drawLine(cs, margin, 757f, pageWidth - margin, 757f, 1.5f);
                drawLine(cs, margin, 753f, pageWidth - margin, 753f, 0.4f);

                // 문서 메타 정보
                // 발급 시각을 서버 현재 시각으로 기록 (요청 시각 = PDF 생성 시각)
                String issuedAt = LocalDateTime.now()
                        .format(DateTimeFormatter.ofPattern("yyyy년 MM월 dd일 HH:mm:ss"));
                drawText(cs, font, 10f, "문서번호  :  " + req.getLoanId(), margin, 735f);
                drawText(cs, font, 10f, "발급일시  :  " + issuedAt, margin, 718f);

                drawLine(cs, margin, 704f, pageWidth - margin, 704f, 0.5f);

                // 차주 정보 섹션
                drawText(cs, font, 11f, "■  차주 정보", margin, 686f);
                drawLabelValue(cs, font, "성              명", req.getBorrowerName(), margin, 665f);

                drawLine(cs, margin, 651f, pageWidth - margin, 651f, 0.5f);

                // 대출 내역 섹션
                drawText(cs, font, 11f, "■  대출 내역", margin, 633f);

                // 금액: BigDecimal → long 변환 후 %,d 포맷으로 천 단위 쉼표 삽입
                String amountStr = String.format("%,d 원", req.getExecuteAmount().longValue());

                // 금리: stripTrailingZeros()로 4.50 → 4.5 처리, toPlainString()으로 1E+1 같은 지수 표기 방지
                String rateStr = req.getInterestRate().stripTrailingZeros().toPlainString() + "% (고정)";

                String periodStr = req.getRepaymentPeriod() + " 개월";
                String monthlyStr = String.format("%,d 원", req.getMonthlyPayment().longValue());

                drawLabelValue(cs, font, "대출 상품명", req.getLoanProductName(), margin, 613f);
                drawLabelValue(cs, font, "대  출  금액", amountStr, margin, 595f);
                drawLabelValue(cs, font, "적  용  금리", rateStr, margin, 577f);
                drawLabelValue(cs, font, "대  출  기간", periodStr, margin, 559f);
                drawLabelValue(cs, font, "월  상  환금", monthlyStr, margin, 541f);
                drawLabelValue(cs, font, "첫  상  환일", req.getRepaymentStartDate(), margin, 523f);
                drawLabelValue(cs, font, "만    기    일", req.getMaturityDate(), margin, 505f);

                drawLine(cs, margin, 491f, pageWidth - margin, 491f, 0.5f);

                // 입금 정보 섹션
                drawText(cs, font, 11f, "■  입금 정보", margin, 473f);
                drawLabelValue(cs, font, "입  금  은행", req.getDepositBankName(), margin, 453f);
                // 계좌번호는 개인정보 보호를 위해 마스킹하여 출력
                drawLabelValue(cs, font, "입  금  계좌", maskAccount(req.getDepositAccountNo()), margin, 435f);
                drawLabelValue(cs, font, "입금 거래번호", req.getDepositTransactionId(), margin, 417f);

                // 하단 구분선 (상단과 동일한 이중선, 순서만 반전)
                drawLine(cs, margin, 400f, pageWidth - margin, 400f, 0.4f);
                drawLine(cs, margin, 396f, pageWidth - margin, 396f, 1.5f);

                // 안내 문구
                drawCenteredText(cs, font, 9f, "본 확인서는 대출 실행 증빙 서류로 활용하실 수 있습니다.", pageWidth, 376f);
                drawCenteredText(cs, font, 9f, "위·변조 시 관련 법령에 따라 처벌받을 수 있습니다.", pageWidth, 361f);

                drawLine(cs, margin, 348f, pageWidth - margin, 348f, 0.3f);

                // 실제 은행명과 함께 대리업무 경유 발급임을 명시
                drawCenteredText(cs, font, 9f, req.getDepositBankName() + "  |  대리업무 취급기관 경유 발급", pageWidth, 332f);
            }

            // PDF 내용을 메모리 버퍼에 직렬화
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            doc.save(baos);
            // 바이트 배열로 변환하여 반환 — 컨트롤러에서 HTTP 응답 바디로 사용
            return baos.toByteArray();

        } catch (IOException e) {
            log.error("[PDF] 대출 실행 확인서 생성 실패", e);
            throw new RuntimeException("대출 실행 확인서 PDF 생성에 실패했습니다.", e);
        }
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

    /**
     * PDF에 텍스트 한 줄을 절대 좌표(x, y)에 출력
     */
    private void drawText(PDPageContentStream cs, PDType0Font font, float size, String text, float x, float y) throws IOException {
        cs.beginText();
        cs.setFont(font, size);
        // newLineAtOffset: 현재 텍스트 위치 기준 상대 이동이지만,
        // beginText() 직후에는 (0,0)이므로 사실상 절대 좌표로 동작
        cs.newLineAtOffset(x, y);
        cs.showText(text);
        cs.endText();
    }

    /**
     * 텍스트를 페이지 가로 중앙에 출력
     */
    private void drawCenteredText(PDPageContentStream cs, PDType0Font font, float size, String text, float pageWidth, float y) throws IOException {
        float textWidth = font.getStringWidth(text) / 1000 * size;
        float x = (pageWidth - textWidth) / 2;
        drawText(cs, font, size, text, x, y);
    }

    /**
     * 지정한 두 좌표 사이에 수평선을 그리기
     */
    private void drawLine(PDPageContentStream cs, float x1, float y1, float x2, float y2, float lineWidth) throws IOException {
        cs.setLineWidth(lineWidth);
        cs.moveTo(x1, y1);
        cs.lineTo(x2, y2);
        cs.stroke();
    }

    /** 라벨과 값을 "라벨  :  값" 형식으로 한 줄에 출력한다. */
    private void drawLabelValue(PDPageContentStream cs, PDType0Font font, String label, String value, float margin, float y) throws IOException {
        drawText(cs, font, 10f, label + "  :  " + value, margin, y);
    }

    /**
     * 계좌번호를 마스킹하여 반환
     */
    private String maskAccount(String accountNo) {
        if (accountNo == null || accountNo.length() < 6) return accountNo;
        // 하이픈 등 숫자 외 문자 제거
        String digits = accountNo.replaceAll("[^0-9]", "");
        if (digits.length() < 6) return accountNo;
        return digits.substring(0, 3) + "-***-***" + digits.substring(digits.length() - 3);
    }

}

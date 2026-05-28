package com.woorifisan.platform.domain.loan.controller;

import com.woorifisan.platform.global.response.ApiResponse;
import com.woorifisan.platform.domain.loan.dto.response.LoanContractDocumentsResponse;
import com.woorifisan.platform.domain.loan.dto.request.LoanEvaluateRequest;
import com.woorifisan.platform.domain.loan.dto.response.LoanDocumentUploadResponse;
import com.woorifisan.platform.domain.loan.dto.response.LoanEvaluateResponse;
import com.woorifisan.platform.domain.loan.dto.response.LoanEvaluationResultResponse;
import com.woorifisan.platform.domain.loan.dto.request.LoanExecuteRequest;
import com.woorifisan.platform.domain.loan.dto.response.LoanExecuteResponse;
import com.woorifisan.platform.domain.loan.dto.response.LoanRequiredDocumentsResponse;
import com.woorifisan.platform.domain.loan.service.LoanService;
import com.woorifisan.platform.global.config.swagger.CustomExceptionDescription;
import com.woorifisan.platform.global.config.swagger.SwaggerResponseDescription;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 대출 중개 컨트롤러
 *
 * ── 역할 ──────────────────────────────────────────────────────────────────
 * 플랫폼(중개사)이 프론트엔드와 은행 코어 사이에 위치하는 Pass-through 라우터.
 * 민감 데이터(주민번호, 계좌번호 등)는 JWE 암호화 상태 그대로 전달하며
 * 플랫폼은 절대 복호화하지 않는다 (Zero-Knowledge 원칙).
 *
 * ── 공통 파라미터 — @AuthenticationPrincipal Long staffId ────────────────
 * Spring Security 필터 체인(JwtAuthenticationFilter)이 요청의
 * "Authorization: Bearer <JWT>" 헤더를 파싱해 JWT sub 클레임에서
 * staffId(Long)를 꺼낸 뒤 SecurityContextHolder에 저장한다.
 * @AuthenticationPrincipal은 SecurityContext의 principal을 꺼내
 * 파라미터에 자동 주입한다 — 컨트롤러가 SecurityContext를 직접 참조하지
 * 않아도 되므로 코드가 간결하고 테스트하기 쉬워진다.
 *
 * ── 엔드포인트 목록 ────────────────────────────────────────────────────────
 *   [1] GET  /api/v1/loan/review/documents                          → 심사 서류 조회
 *   [2] POST /api/v1/loan/evaluation                                → 심사 신청
 *   [3] GET  /api/v1/loan/evaluation/{applicationId}/stream (SSE)  → 심사 결과 스트리밍
 *   [4] GET  /api/v1/loan/contract/documents/{productCode}/{evalId} → 계약 서류 조회
 *   [5] POST /api/v1/loan/contract/execution                        → 대출 실행
 */
@Tag(name = "Bank Loan", description = "은행 대출 중개 API")
@Validated  // 클래스 레벨에 선언 → @PathVariable 등 메서드 파라미터의 @NotBlank도 Bean Validation 대상이 됨
@RestController
@RequestMapping("/api/v1/loan")
@RequiredArgsConstructor
public class LoanController {

    private final LoanService loanService;

    /**
     * [1] 심사 서류 조회 (Step 1)
     * GET /api/v1/loan/review/documents
     *
     * 심사 신청 전 고객에게 안내할 필수/선택 동의 서류 목록을 반환한다.
     * 프론트엔드는 이 목록을 화면에 보여주고, 고객이 동의한 항목들을
     * Step 2 심사 요청 body(documents[])에 담아 전송한다.
     */
    @Operation(summary = "심사 서류 조회", description = "대출 심사 신청 전 필요한 서류 목록을 조회합니다.")
    @CustomExceptionDescription(SwaggerResponseDescription.BANK_LOAN)
    @GetMapping("/review/documents")
    public ApiResponse<LoanRequiredDocumentsResponse> getRequiredDocuments(
            @AuthenticationPrincipal Long staffId) {

        return ApiResponse.success(loanService.getRequiredDocuments(staffId));
    }

    /**
     * [1-5] 서류 업로드
     * POST /api/v1/loan/documents
     *
     * 심사 신청 전 필수 서류(PDF)를 플랫폼 서버에 업로드한다.
     * 성공 시 documentId를 반환하며, 심사 신청 시 이 ID를 함께 전송한다.
     */
    @Operation(summary = "서류 업로드", description = "대출 심사 신청 전 필수 서류를 업로드합니다.")
    @PostMapping(value = "/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<LoanDocumentUploadResponse> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal Long staffId) {

        return ApiResponse.success(loanService.uploadDocument(file, staffId));
    }

    /**
     * [2] 심사 신청 (Step 2)
     * POST /api/v1/loan/evaluation
     *
     * 고객 정보와 약관 동의 목록을 받아 은행 코어에 신용 심사를 요청한다.
     * 심사 자체는 비동기로 처리되므로 이 API는 applicationId만 즉시 반환한다.
     * 프론트엔드는 반환된 applicationId로 [3] SSE 엔드포인트를 구독해
     * 심사 결과(APPROVED / REJECTED)를 비동기로 수신한다.
     *
     * @Valid — LoanEvaluateRequest 필드의 Bean Validation(@NotBlank 등)을 자동 실행.
     *          유효성 검사 실패 시 MethodArgumentNotValidException 발생 → GlobalExceptionHandler 처리.
     */
    @Operation(summary = "심사 신청", description = "고객의 대출 심사를 신청합니다. 결과는 SSE를 통해 비동기로 전달됩니다.")
    @CustomExceptionDescription(SwaggerResponseDescription.BANK_LOAN)
    @PostMapping("/evaluation")
    public ApiResponse<LoanEvaluateResponse> evaluateLoan(
            @Valid @RequestBody LoanEvaluateRequest request,
            @AuthenticationPrincipal Long staffId) {

        return ApiResponse.success(loanService.evaluateLoan(request, staffId));
    }

    /**
     * [3] 심사 결과 실시간 수신 (Step 3) — SSE(Server-Sent Events)
     * GET /api/v1/loan/evaluation/{applicationId}/stream
     *
     * ── SSE를 사용하는 이유 ───────────────────────────────────────────────
     * 심사는 수초~수십 초 걸리므로 프론트가 완료 시점을 알 수 없다.
     * 일반 HTTP는 "요청 1회 → 응답 1회 → 연결 종료" 구조이므로 적합하지 않다.
     * SSE는 서버가 커넥션을 유지하면서 준비되는 즉시 데이터를 push할 수 있다.
     *
     * ── produces = TEXT_EVENT_STREAM_VALUE 가 필요한 이유 ────────────────
     * 브라우저(EventSource API)는 Content-Type이 "text/event-stream"인
     * 응답에 한해 스트리밍 모드로 동작한다. 이 값을 명시하지 않으면
     * Spring이 application/json으로 응답해 브라우저가 일반 응답으로 처리하고
     * 연결을 끊어버린다. SseEmitter가 emitter.send()를 호출할 때마다
     * Spring은 "data: ...\n\n" 형식의 청크를 HTTP 커넥션을 통해 클라이언트로 밀어낸다.
     *
     * ── 흐름 ─────────────────────────────────────────────────────────────
     * 1. 연결 즉시 PENDING 이벤트 전송 (심사 중 표시)
     * 2. 심사 완료 후 APPROVED / REJECTED / FAILED 이벤트 전송
     * 3. emitter.complete() 호출 → 연결 종료
     *
     * @PathVariable applicationId — [2]에서 받은 심사 신청 번호
     */
    @Operation(summary = "심사 결과 스트리밍", description = "SSE를 통해 대출 심사 결과를 실시간으로 수신합니다.")
    @GetMapping(value = "/evaluation/{applicationId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamEvaluationResult(
            @PathVariable @NotBlank(message = "신청 ID는 필수입니다.") String applicationId,
            @AuthenticationPrincipal Long staffId) {

        return loanService.streamEvaluationResult(applicationId, staffId);
    }

    /**
     * [4] 계약 서류 조회 (Step 5)
     * GET /api/v1/loan/contract/documents/{loanProductCode}/{evaluationId}
     *
     * 고객이 상품을 선택한 후 서명/동의가 필요한 계약 서류 목록을 반환한다.
     * 경로 변수 2개를 사용하는 이유: 어떤 심사 건(evaluationId)의
     * 어떤 상품(loanProductCode)에 대한 계약 서류인지 모두 식별해야 하기 때문이다.
     *
     * @PathVariable loanProductCode — [3] SSE 결과에서 고객이 선택한 상품 코드
     * @PathVariable evaluationId    — [3] SSE 결과의 심사 고유 ID
     */
    @Operation(summary = "계약 서류 조회", description = "대출 승인 후 계약 체결에 필요한 서류 목록을 조회합니다.")
    @CustomExceptionDescription(SwaggerResponseDescription.BANK_LOAN)
    @GetMapping("/contract/documents/{loanProductCode}/{evaluationId}")
    public ApiResponse<LoanContractDocumentsResponse> getContractDocuments(
            @PathVariable @NotBlank(message = "상품 코드는 필수입니다.") String loanProductCode,
            @PathVariable @NotBlank(message = "심사 ID는 필수입니다.") String evaluationId,
            @AuthenticationPrincipal Long staffId) {

        return ApiResponse.success(loanService.getContractDocuments(loanProductCode, evaluationId, staffId));
    }

    /**
     * [5] 대출 실행 (Step 6)
     * POST /api/v1/loan/contract/execution
     *
     * 계약 서류 동의가 완료된 후 실제로 대출금을 고객 계좌에 입금시키는 최종 단계.
     * 계좌번호와 비밀번호는 JWE 암호화 상태로 받아 그대로 은행 코어에 전달한다
     * (플랫폼이 복호화할 수 없도록 Zero-Knowledge 원칙 준수).
     * 성공 시 loanId, 월 상환금, 만기일 등 대출 확정 정보를 반환한다.
     */
    @Operation(summary = "대출 실행", description = "최종 계약 동의 후 대출을 실행합니다.")
    @CustomExceptionDescription(SwaggerResponseDescription.BANK_LOAN)
    @PostMapping("/contract/execution")
    public ApiResponse<LoanExecuteResponse> executeLoan(
            @Valid @RequestBody LoanExecuteRequest request,
            @AuthenticationPrincipal Long staffId) {

        return ApiResponse.success(loanService.executeLoan(request, staffId));
    }
}

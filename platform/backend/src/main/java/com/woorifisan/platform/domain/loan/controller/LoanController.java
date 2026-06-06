package com.woorifisan.platform.domain.loan.controller;

import com.woorifisan.platform.global.response.ApiResponse;
import com.woorifisan.platform.domain.loan.dto.request.LoanCallbackRequest;
import com.woorifisan.platform.domain.loan.dto.request.LoanEvaluateRequest;
import com.woorifisan.platform.domain.loan.dto.response.LoanContractDocumentsResponse;
import com.woorifisan.platform.domain.loan.dto.response.LoanEvaluateResponse;
import com.woorifisan.platform.domain.loan.dto.response.LoanEvaluationResultResponse;
import com.woorifisan.platform.domain.loan.dto.request.LoanExecuteRequest;
import com.woorifisan.platform.domain.loan.dto.response.LoanExecuteResponse;
import com.woorifisan.platform.domain.loan.dto.response.LoanRequiredDocumentsResponse;
import org.springframework.http.HttpStatus;
import com.woorifisan.platform.domain.loan.service.LoanService;
import com.woorifisan.platform.global.config.swagger.CustomExceptionDescription;
import com.woorifisan.platform.global.config.swagger.SwaggerResponseDescription;
import com.woorifisan.platform.global.security.annotation.VerifyTerminalSignature;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Tag(name = "Bank Loan", description = "은행 대출 중개 API")
@Validated
@RestController
@RequestMapping("/api/v1/loan")
@RequiredArgsConstructor
public class LoanController {

    private final LoanService loanService;

    // 심사 서류 조회
    @Operation(summary = "심사 서류 조회", description = "대출 심사 신청 전 필요한 서류 목록을 조회합니다.")
    @CustomExceptionDescription(SwaggerResponseDescription.BANK_LOAN)
    @GetMapping("/review/documents")
    public ApiResponse<LoanRequiredDocumentsResponse> getRequiredDocuments(
            // JWT 토큰에서 추출된 staffId — SecurityContext에서 자동 주입
            @AuthenticationPrincipal Long staffId) {
        return ApiResponse.success(loanService.getRequiredDocuments(staffId));
    }

    // SSE 구독 (심사 신청보다 반드시 먼저 호출)
    @Operation(summary = "심사 결과 SSE 구독",
               description = "프론트엔드가 [신청하기] 클릭 시 requestKey로 SSE 채널을 열고 은행 콜백 결과를 수신합니다.")
    // 이 헤더가 없으면 브라우저가 SSE로 인식하지 않음
    @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(
            // 프론트가 생성한 UUID — 이 키로 emitter를 Map에 등록하고, 나중에 webhook이 오면 꺼내서 push
            @RequestParam @NotBlank(message = "requestKey는 필수입니다.") String requestKey) {
        return loanService.subscribe(requestKey);
    }

    // 심사 결과 직접 조회 (SSE 실패 시 polling fallback)
    @Operation(summary = "심사 결과 직접 조회 (Polling fallback)",
               description = "SSE 연결 실패 시 Redis 캐시에서 결과를 조회합니다. 결과 없으면 204, 있으면 200 반환.")
    @GetMapping("/result")
    public ResponseEntity<ApiResponse<LoanEvaluationResultResponse>> getResult(
            @RequestParam @NotBlank(message = "requestKey는 필수입니다.") String requestKey,
            @AuthenticationPrincipal Long staffId) {
        LoanEvaluationResultResponse result = loanService.getResult(requestKey);
        if (result == null) {
            return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
        }
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    // 대출 심사 신청
    @Operation(summary = "심사 신청 (multipart)",
               description = "파일 + JSON 데이터를 동시에 은행으로 전달. 은행이 즉시 loanNo를 반환하고, 심사는 @Async 후 Webhook으로 결과 통보.")
    @CustomExceptionDescription(SwaggerResponseDescription.BANK_LOAN)
    @VerifyTerminalSignature
    // JSON + 파일을 한 요청에 받기 위해 multipart 선언
    @PostMapping(value = "/evaluation", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<LoanEvaluateResponse> evaluateLoan(
            @RequestHeader("x-bank-key-id") String bankKeyId,
            // multipart의 data 파트를 JSON으로 역직렬화 + @Valid 검증
            @RequestPart("data") @Valid LoanEvaluateRequest request,
            // multipart의 files 파트들을 List로 수집
            @RequestPart("files") List<MultipartFile> files,
            @AuthenticationPrincipal Long staffId) {

        // 헤더의 키 ID를 바디 필드에 주입하여 은행이 식별할 수 있도록 함
        request.setBankKeyId(bankKeyId);

        return ApiResponse.success(loanService.evaluateLoan(request, files, staffId));
    }

    // 은행 Webhook 수신
    @Operation(summary = "심사 결과 Webhook 수신 (은행→플랫폼)",
               description = "은행 @Async 심사 완료 후 결과를 플랫폼에 통보. X-Webhook-Secret 헤더로 인증.")
    // Security 필터에서 이 엔드포인트는 JWT 검증 없이 통과시킴 (호출자가 은행 서버이므로)
    // 대신 X-Webhook-Secret 헤더로 위조 요청을 차단
    @PostMapping("/callback")
    public ResponseEntity<Void> handleCallback(
            @RequestBody LoanCallbackRequest callback,
            // HTTP 헤더 값을 파라미터로 바인딩
            @RequestHeader("X-Webhook-Secret") String secret) {
        loanService.handleCallback(callback, secret);
        // Bank는 200 OK만 확인. 바디 없이 반환
        return ResponseEntity.ok().build();
    }

    // 계약 서류 조회
    @Operation(summary = "계약 서류 조회", description = "대출 승인 후 계약 체결에 필요한 서류 목록을 조회합니다.")
    @CustomExceptionDescription(SwaggerResponseDescription.BANK_LOAN)
    @GetMapping("/contract/documents/{loanProductCode}/{loanNo}")
    public ApiResponse<LoanContractDocumentsResponse> getContractDocuments(
            @PathVariable @NotBlank(message = "상품 코드는 필수입니다.") String loanProductCode,
            @PathVariable @NotBlank(message = "대출 계약번호는 필수입니다.") String loanNo,
            @AuthenticationPrincipal Long staffId) {
        return ApiResponse.success(loanService.getContractDocuments(loanProductCode, loanNo, staffId));
    }

    // 대출 실행
    @Operation(summary = "대출 실행", description = "최종 계약 동의 후 대출을 실행합니다.")
    @CustomExceptionDescription(SwaggerResponseDescription.BANK_LOAN)
    @VerifyTerminalSignature
    @PostMapping("/contract/execution")
    public ApiResponse<LoanExecuteResponse> executeLoan(
            @RequestHeader("x-bank-key-id") String bankKeyId,
            @Valid @RequestBody LoanExecuteRequest request,
            @AuthenticationPrincipal Long staffId) {

        // 헤더의 키 ID를 바디 필드에 주입하여 은행이 식별할 수 있도록 함
        request.setBankKeyId(bankKeyId);

        return ApiResponse.success(loanService.executeLoan(request, staffId));
    }

}


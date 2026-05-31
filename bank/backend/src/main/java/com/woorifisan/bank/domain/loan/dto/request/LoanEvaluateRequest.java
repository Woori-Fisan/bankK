package com.woorifisan.bank.domain.loan.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;

// Platform → Bank 로 전달되는 대출 심사 요청 DTO
// Platform이 프론트로부터 받은 값을 그대로 Bank에 multipart/form-data 로 포워딩
@Getter
@NoArgsConstructor // JSON 역직렬화 시 기본 생성자가 필요
public class LoanEvaluateRequest {

    @NotBlank
    private String customerName;       // 고객 성명

    @NotBlank
    private String customerRrnPrefix;  // 주민등록번호 앞 7자리

    // Bank 자신의 bank.code 와 일치해야됨
    @NotBlank
    private String depositBankCode;

    // 평문 계좌번호
    @NotBlank
    private String depositAccountNo;

    // 최소 100,000원 이상 신청 가능
    @NotNull
    @DecimalMin("100000")
    private BigDecimal requestedAmount; // 고객 신청 금액

    // 최소 1개월 이상
    @NotNull
    @Min(1)
    private Integer requestedPeriod;    // 신청 상환 기간 (개월)

    @NotNull
    private Boolean isCreditInfoAgreed;    // 개인 신용 정보 이용 동의

    @NotNull
    private Boolean isProductTermsAgreed;  // 상품 약관 동의

    @NotNull
    private Boolean isDocumentCollected;   // 서류 징구 완료

    @NotBlank
    private String requestKey;
}

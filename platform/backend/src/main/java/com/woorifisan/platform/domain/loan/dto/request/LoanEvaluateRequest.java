package com.woorifisan.platform.domain.loan.dto.request;

import com.woorifisan.platform.domain.loan.dto.response.LoanDocumentDto;
import com.woorifisan.platform.global.security.dto.SecureRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.math.BigDecimal;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * 암호화된 대출 심사 요청 DTO (Pass-through)
 * 플랫폼은 이 데이터를 복호화하지 않고 은행으로 전달만 합니다.
 */
@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class LoanEvaluateRequest extends SecureRequest {

    /** SSE 구독과 결과 매핑에 사용되는 프론트 생성 UUID */
    @NotBlank(message = "requestKey는 필수입니다.")
    private String requestKey;

    /** 심사 요청을 라우팅할 은행 코드 */
    @NotBlank(message = "은행 코드는 필수입니다.")
    private String bankCode;

    /** 대출금을 받을 계좌의 은행 코드 */
    @NotBlank(message = "입금 은행 코드는 필수입니다.")
    private String depositBankCode;

    /** 신청 금액 */
    private BigDecimal requestedAmount;

    /** 신청 상환 기간 (개월) */
    private Integer requestedPeriod;

    /** 고객이 동의한 약관 서류 목록 */
    @Valid
    @NotEmpty(message = "동의 서류 목록은 필수입니다.")
    private List<LoanDocumentDto> documents;
}

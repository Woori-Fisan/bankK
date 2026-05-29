package com.woorifisan.platform.domain.loan.dto.request;

import com.woorifisan.platform.domain.loan.dto.response.LoanDocumentDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.math.BigDecimal;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class LoanEvaluateRequest {

    /** SSE 구독과 결과 매핑에 사용되는 프론트 생성 UUID */
    @NotBlank(message = "requestKey는 필수입니다.")
    private String requestKey;

    /** 심사 요청을 라우팅할 은행 코드 (예: 020 = 우리은행) */
    @NotBlank(message = "은행 코드는 필수입니다.")
    private String bankCode;

    /** 고객 실명 (마스킹 처리 후 로그에 기록됨) */
    @NotBlank(message = "고객명은 필수입니다.")
    private String customerName;

    /** 암호화된 주민번호 앞 7자리 */
    @NotBlank(message = "주민번호 암호문은 필수입니다.")
    private String customerRrnPrefix;

    /** 고객 휴대폰 번호 */
    @NotBlank(message = "연락처는 필수입니다.")
    private String customerPhone;

    /** 대출금을 받을 계좌의 은행 코드 (예: 020 = 우리은행) */
    @NotBlank(message = "입금 은행 코드는 필수입니다.")
    private String depositBankCode;

    /** 암호화된 입금 계좌번호 */
    @NotBlank(message = "입금 계좌번호 암호문은 필수입니다.")
    private String depositAccountNo;

    /** 신청 금액 */
    private BigDecimal requestedAmount;

    /** 신청 상환 기간 (개월) */
    private Integer requestedPeriod;

    /** 고객이 동의한 약관 서류 목록 */
    @Valid
    @NotEmpty(message = "동의 서류 목록은 필수입니다.")
    private List<LoanDocumentDto> documents;
}

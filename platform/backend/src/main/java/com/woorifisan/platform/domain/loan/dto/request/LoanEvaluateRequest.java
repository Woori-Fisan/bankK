package com.woorifisan.platform.domain.loan.dto.request;

import com.woorifisan.platform.domain.loan.dto.response.LoanDocumentDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
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
/**
 * 대출 심사 신청 요청 — 프론트엔드가 Step 2에서 보내는 데이터
 * 민감 필드(주민번호, 계좌번호)는 JWE 암호화 상태로 수신하며 플랫폼은 복호화하지 않고
 * 그대로 은행 코어로 전달한다 (Zero-Knowledge).
 */
public class LoanEvaluateRequest {

    /** 심사 요청을 라우팅할 은행 코드 (예: 020 = 우리은행) */
    @NotBlank(message = "은행 코드는 필수입니다.")
    private String bankCode;

    /** 고객 실명 (마스킹 처리 후 로그에 기록됨) */
    @NotBlank(message = "고객명은 필수입니다.")
    private String customerName;

    /** JWE 암호화된 주민번호 앞 7자리 (Zero-Knowledge: 플랫폼 복호화 금지) */
    @NotBlank(message = "주민번호 암호문은 필수입니다.")
    private String customerRrnPrefix;

    /** 고객 휴대폰 번호 */
    @NotBlank(message = "연락처는 필수입니다.")
    private String customerPhone;

    /** 대출금을 받을 계좌의 은행 코드 (예: 020 = 우리은행) */
    @NotBlank(message = "입금 은행 코드는 필수입니다.")
    private String depositBankCode;

    /** JWE 암호화된 입금 계좌번호 (Zero-Knowledge: 플랫폼 복호화 금지) */
    @NotBlank(message = "입금 계좌번호 암호문은 필수입니다.")
    private String depositAccountNo;

    /** 고객이 동의한 약관 서류 목록 */
    @Valid
    @NotEmpty(message = "동의 서류 목록은 필수입니다.")
    private List<LoanDocumentDto> documents;

    /** 플랫폼에 업로드된 서류의 documentId 목록 */
    private List<String> uploadedDocumentIds;
}

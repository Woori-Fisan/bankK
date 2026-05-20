package com.woorifisan.platform.domain.loan.dto.request;

import com.woorifisan.platform.domain.loan.dto.response.LoanDocumentDto;
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
public class LoanEvaluateRequest {

    @NotBlank(message = "고객명은 필수입니다.")
    private String customerName;

    /** JWE 암호화된 주민번호 앞 7자리 (Zero-Knowledge: 플랫폼 복호화 금지) */
    @NotBlank(message = "주민번호 암호문은 필수입니다.")
    private String customerRrnPrefix;

    @NotBlank(message = "연락처는 필수입니다.")
    private String customerPhone;

    @NotBlank(message = "입금 은행 코드는 필수입니다.")
    private String depositBankCode;

    /** JWE 암호화된 입금 계좌번호 (Zero-Knowledge: 플랫폼 복호화 금지) */
    @NotBlank(message = "입금 계좌번호 암호문은 필수입니다.")
    private String depositAccountNo;

    @NotEmpty(message = "동의 서류 목록은 필수입니다.")
    private List<LoanDocumentDto> documents;
}

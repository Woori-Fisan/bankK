package com.woorifisan.platform.domain.loan.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class LoanExecuteRequest {

    @NotBlank(message = "은행 코드는 필수입니다.")
    private String bankCode;

    @NotBlank(message = "심사 ID는 필수입니다.")
    private String evaluationId;

    @NotBlank(message = "상품 코드는 필수입니다.")
    private String productCode;

    @Min(value = 1, message = "대출 기간은 1개월 이상이어야 합니다.")
    private int period;

    /** 프론트에서 생성한 1회용 AES 키를 은행 RSA 공개키로 암호화한 값 (Zero-Knowledge: 플랫폼 복호화 금지) */
    @NotBlank(message = "암호화된 AES 키는 필수입니다.")
    private String encryptedKey;

    /** AES로 암호화된 계좌 비밀번호 (Zero-Knowledge: 플랫폼 복호화 금지) */
    @NotBlank(message = "암호화된 계좌 비밀번호는 필수입니다.")
    private String encryptedAccountPassword;

    @NotEmpty(message = "약관 동의 목록은 필수입니다.")
    private List<String> agreedTermsList;
}

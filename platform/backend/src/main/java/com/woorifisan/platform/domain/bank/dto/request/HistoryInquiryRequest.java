package com.woorifisan.platform.domain.bank.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class HistoryInquiryRequest {

    @NotBlank(message = "암호화된 AES 키는 필수입니다.")
    private String encryptedKey;

    @NotBlank(message = "JWS 전자서명은 필수입니다.")
    private String jwsSignature;

    @NotBlank(message = "은행 코드는 필수입니다.")
    private String bankCode;

    @NotBlank(message = "계좌번호는 필수입니다.")
    private String accountNo;

    @NotBlank(message = "주민번호 앞 7자리는 필수입니다.")
    private String customerRrnPrefix;

    @NotBlank(message = "조회 시작일은 필수입니다.")
    private String startDate;

    @NotBlank(message = "조회 종료일은 필수입니다.")
    private String endDate;

    @NotNull(message = "페이지 번호는 필수입니다.")
    private Integer page;

    @NotNull(message = "페이지당 건수는 필수입니다.")
    private Integer size;
}
